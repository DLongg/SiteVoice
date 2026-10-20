package com.sitevoice.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sitevoice.app.audio.AudioPlayer
import com.sitevoice.app.data.NoteEntity
import com.sitevoice.app.data.NoteRepository
import com.sitevoice.app.llm.GroqLlmClient
import com.sitevoice.app.llm.NoteClassifier
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.stt.GroqWhisper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class CurrentSttStatus {
    object Idle : CurrentSttStatus()
    object Transcribing : CurrentSttStatus()
    data class Success(val text: String) : CurrentSttStatus()
    data class Error(val message: String) : CurrentSttStatus()
    object NoApiKey : CurrentSttStatus()
}

class MainViewModel(
    private val repository: NoteRepository,
    private val securePreferences: SecurePreferences,
    private val groqWhisper: GroqWhisper = GroqWhisper(apiKeyProvider = { securePreferences.groqApiKey }),
    private val classifier: NoteClassifier = NoteClassifier(GroqLlmClient(apiKeyProvider = { securePreferences.groqApiKey }))
) : ViewModel() {

    val audioPlayer = AudioPlayer()

    val currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val activeNotes: StateFlow<List<NoteEntity>> = repository.getActiveNotesByDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentSttStatus = MutableStateFlow<CurrentSttStatus>(CurrentSttStatus.Idle)
    val currentSttStatus: StateFlow<CurrentSttStatus> = _currentSttStatus.asStateFlow()

    private val _lastCreatedNoteId = MutableStateFlow<Long?>(null)
    val lastCreatedNoteId: StateFlow<Long?> = _lastCreatedNoteId.asStateFlow()

    fun onRecordFinished(audioFile: File, durationMs: Long) {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            createdAt = now,
            date = currentDate,
            audioPath = audioFile.absolutePath,
            durationMs = durationMs,
            status = "RECORDED"
        )

        viewModelScope.launch {
            val noteId = repository.insertNote(note)
            _lastCreatedNoteId.value = noteId
            triggerStt(noteId, audioFile)
        }
    }

    fun retryStt(note: NoteEntity) {
        val file = File(note.audioPath)
        if (!file.exists()) {
            viewModelScope.launch {
                repository.updateNote(note.copy(error = "Tệp âm thanh không tồn tại"))
            }
            return
        }
        viewModelScope.launch {
            repository.updateNote(note.copy(error = null))
            triggerStt(note.id, file)
        }
    }

    private suspend fun triggerStt(noteId: Long, audioFile: File) {
        val apiKey = securePreferences.groqApiKey
        if (apiKey.isNullOrBlank()) {
            _currentSttStatus.value = CurrentSttStatus.NoApiKey
            val currentNote = repository.getNoteById(noteId)
            if (currentNote != null) {
                repository.updateNote(currentNote.copy(error = "Chưa cài đặt API Key"))
            }
            return
        }

        _currentSttStatus.value = CurrentSttStatus.Transcribing
        val result = groqWhisper.transcribe(audioFile)

        val currentNote = repository.getNoteById(noteId) ?: return

        if (result.isSuccess) {
            val text = result.getOrThrow()
            _currentSttStatus.value = CurrentSttStatus.Success(text)
            repository.updateNote(
                currentNote.copy(
                    text = text,
                    sttSource = "groq",
                    status = "TRANSCRIBED",
                    error = null
                )
            )
            // Tự động phân loại ghi chú bằng LLM sau khi STT xong
            triggerClassification(noteId, text)
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi chuyển đổi giọng nói"
            _currentSttStatus.value = CurrentSttStatus.Error(errorMsg)
            repository.updateNote(
                currentNote.copy(
                    error = errorMsg
                )
            )
        }
    }

    private fun triggerClassification(noteId: Long, text: String) {
        viewModelScope.launch {
            val classifyResult = classifier.classify(noteId, text)
            val currentNote = repository.getNoteById(noteId) ?: return@launch

            if (classifyResult.isSuccess) {
                val label = classifyResult.getOrThrow().label
                repository.updateNote(
                    currentNote.copy(
                        label = label,
                        labelSource = "auto",
                        status = "LABELED"
                    )
                )
            } else {
                // Fallback nếu LLM phân loại lỗi: gán nhãn chua_phan_loai
                repository.updateNote(
                    currentNote.copy(
                        label = NoteClassifier.LABEL_CHUA_PHAN_LOAI,
                        labelSource = "auto",
                        status = "LABELED"
                    )
                )
            }
        }
    }

    fun updateNoteText(noteId: Long, newText: String) {
        viewModelScope.launch {
            repository.updateText(noteId, newText)
        }
    }

    fun updateNoteLabel(noteId: Long, newLabel: String, labelSource: String = "user") {
        viewModelScope.launch {
            repository.updateLabel(noteId, newLabel, labelSource)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.softDelete(noteId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}

class MainViewModelFactory(
    private val repository: NoteRepository,
    private val securePreferences: SecurePreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, securePreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
