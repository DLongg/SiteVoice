package com.sitevoice.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sitevoice.app.audio.AudioPlayer
import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.DailyInputRepository
import com.sitevoice.app.data.DailyReportEntity
import com.sitevoice.app.data.DailyReportRepository
import com.sitevoice.app.data.NoteEntity
import com.sitevoice.app.data.NoteRepository
import com.sitevoice.app.export.GoogleAuthManager
import com.sitevoice.app.export.GoogleDocsExporter
import com.sitevoice.app.llm.GroqLlmClient
import com.sitevoice.app.llm.NoteClassifier
import com.sitevoice.app.llm.ReportSynthesizer
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.stt.GroqWhisper
import com.sitevoice.app.template.DailyReportContent
import com.sitevoice.app.template.TemplateSchema
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
    private val dailyInputRepository: DailyInputRepository,
    private val dailyReportRepository: DailyReportRepository,
    private val securePreferences: SecurePreferences,
    private val templateSchema: TemplateSchema,
    private val groqWhisper: GroqWhisper = GroqWhisper(apiKeyProvider = { securePreferences.groqApiKey }),
    private val classifier: NoteClassifier = NoteClassifier(GroqLlmClient(apiKeyProvider = { securePreferences.groqApiKey })),
    private val reportSynthesizer: ReportSynthesizer = ReportSynthesizer(GroqLlmClient(apiKeyProvider = { securePreferences.groqApiKey }), templateSchema),
    private val exporter: GoogleDocsExporter = GoogleDocsExporter()
) : ViewModel() {

    val audioPlayer = AudioPlayer()

    val currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val activeNotes: StateFlow<List<NoteEntity>> = repository.getActiveNotesByDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailyInput: StateFlow<DailyInputEntity?> = dailyInputRepository.getDailyInput(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val dailyReport: StateFlow<DailyReportEntity?> = dailyReportRepository.getReport(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isSynthesizing = MutableStateFlow(false)
    val isSynthesizing: StateFlow<Boolean> = _isSynthesizing.asStateFlow()

    private val _synthesisError = MutableStateFlow<String?>(null)
    val synthesisError: StateFlow<String?> = _synthesisError.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

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

    fun reclassifyNote(note: NoteEntity) {
        val text = note.text
        if (text.isNullOrBlank()) return
        triggerClassification(note.id, text)
    }

    private fun triggerClassification(noteId: Long, text: String) {
        viewModelScope.launch {
            android.util.Log.i("SiteVoiceVM", "Bắt đầu phân loại note $noteId: $text")
            val classifyResult = classifier.classify(noteId, text)
            val currentNote = repository.getNoteById(noteId) ?: return@launch

            if (classifyResult.isSuccess) {
                val label = classifyResult.getOrThrow().label
                android.util.Log.i("SiteVoiceVM", "Phân loại thành công note $noteId -> label: $label")
                repository.updateNote(
                    currentNote.copy(
                        label = label,
                        labelSource = "auto",
                        status = "LABELED"
                    )
                )
            } else {
                val err = classifyResult.exceptionOrNull()?.message
                android.util.Log.e("SiteVoiceVM", "Phân loại thất bại note $noteId: $err")
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
            triggerClassification(noteId, newText)
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

    fun saveDailyInput(input: DailyInputEntity) {
        viewModelScope.launch {
            dailyInputRepository.saveDailyInput(input)
        }
    }

    suspend fun getInitialDailyInput(): DailyInputEntity {
        return dailyInputRepository.getDailyInputOrDefault(currentDate)
    }

    fun synthesizeDailyReport(onSuccess: (() -> Unit)? = null) {
        val apiKey = securePreferences.groqApiKey
        if (apiKey.isNullOrBlank()) {
            _synthesisError.value = "Chưa cài đặt Groq API Key trong Cài đặt."
            return
        }

        val notes = activeNotes.value
        if (notes.isEmpty()) {
            _synthesisError.value = "Chưa có ghi chú nào hôm nay để tổng hợp báo cáo."
            return
        }

        viewModelScope.launch {
            _isSynthesizing.value = true
            _synthesisError.value = null
            try {
                val result = reportSynthesizer.synthesizeReport(currentDate, notes)
                if (result.isSuccess) {
                    val json = result.getOrThrow()
                    val reportEntity = DailyReportEntity(
                        date = currentDate,
                        templateId = templateSchema.id,
                        contentJson = json,
                        status = "DRAFT",
                        updatedAt = System.currentTimeMillis()
                    )
                    dailyReportRepository.saveReport(reportEntity)
                    onSuccess?.invoke()
                } else {
                    _synthesisError.value = result.exceptionOrNull()?.message ?: "Lỗi khi tổng hợp báo cáo"
                }
            } catch (e: Exception) {
                _synthesisError.value = e.message ?: "Lỗi hệ thống khi tổng hợp"
            } finally {
                _isSynthesizing.value = false
            }
        }
    }

    fun approveDailyReport() {
        viewModelScope.launch {
            dailyReportRepository.approveReport(currentDate)
        }
    }

    fun clearSynthesisError() {
        _synthesisError.value = null
    }

    fun exportDailyReport(context: Context, onSuccess: (docUrl: String) -> Unit) {
        val account = GoogleAuthManager.getLastSignedInAccount(context)
        if (account == null) {
            _exportError.value = "Chưa đăng nhập tài khoản Google. Vui lòng mở Cài đặt và bấm 'Đăng nhập Google' trước."
            return
        }

        val report = dailyReport.value
        if (report == null) {
            _exportError.value = "Chưa có bản tổng hợp nhật ký để xuất."
            return
        }

        viewModelScope.launch {
            _isExporting.value = true
            _exportError.value = null
            try {
                val acc = account.account
                if (acc == null) {
                    _exportError.value = "Không tìm thấy thông tin tài khoản Google."
                    return@launch
                }
                val tokenResult = GoogleAuthManager.fetchAccessToken(context, acc)
                if (tokenResult.isFailure) {
                    _exportError.value = "Không thể lấy quyền truy cập Google: ${tokenResult.exceptionOrNull()?.message}"
                    return@launch
                }
                val token = tokenResult.getOrThrow()
                val currentDailyInput = dailyInput.value
                val reportContent = DailyReportContent.fromJson(report.contentJson)
                val templateId = securePreferences.docTemplateId

                val exportResult = exporter.exportReport(
                    date = currentDate,
                    dailyInput = currentDailyInput,
                    reportContent = reportContent,
                    templateDocId = templateId,
                    accessToken = token
                )

                if (exportResult.isSuccess) {
                    val result = exportResult.getOrThrow()
                    dailyReportRepository.markExported(currentDate, result.docUrl)
                    onSuccess(result.docUrl)
                } else {
                    _exportError.value = exportResult.exceptionOrNull()?.message ?: "Lỗi khi xuất Google Doc"
                }
            } catch (e: Exception) {
                _exportError.value = e.message ?: "Lỗi hệ thống khi xuất Doc"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearExportError() {
        _exportError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}

class MainViewModelFactory(
    private val repository: NoteRepository,
    private val dailyInputRepository: DailyInputRepository,
    private val dailyReportRepository: DailyReportRepository,
    private val securePreferences: SecurePreferences,
    private val templateSchema: TemplateSchema
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, dailyInputRepository, dailyReportRepository, securePreferences, templateSchema) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
