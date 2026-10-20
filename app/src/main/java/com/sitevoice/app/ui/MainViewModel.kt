package com.sitevoice.app.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.UserRecoverableAuthException
import com.sitevoice.app.audio.AudioPlayer
import com.sitevoice.app.data.AppDatabase
import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.DailyInputRepository
import com.sitevoice.app.data.DailyReportEntity
import com.sitevoice.app.data.DailyReportRepository
import com.sitevoice.app.data.NoteEntity
import com.sitevoice.app.data.NoteRepository
import com.sitevoice.app.export.GoogleAuthManager
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.export.GoogleDocsExporter
import com.sitevoice.app.export.PlaceholderMapper
import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormSetupAnalysis
import com.sitevoice.app.formsetup.FormSetupService
import com.sitevoice.app.llm.GroqLlmClient
import com.sitevoice.app.llm.NoteClassifier
import com.sitevoice.app.llm.ReportSynthesizer
import com.sitevoice.app.queue.ClassifyWorker
import com.sitevoice.app.queue.ExportWorker
import com.sitevoice.app.queue.SttWorker
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.stt.GroqWhisper
import com.sitevoice.app.template.DailyReportContent
import com.sitevoice.app.template.DefaultTemplateRepository
import com.sitevoice.app.template.TemplateRepository
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
    private val appContext: Context,
    private val repository: NoteRepository,
    private val dailyInputRepository: DailyInputRepository,
    private val dailyReportRepository: DailyReportRepository,
    private val securePreferences: SecurePreferences,
    private val templateSchema: TemplateSchema,
    val templateRepository: TemplateRepository = DefaultTemplateRepository(appContext, AppDatabase.getDatabase(appContext).templateDao()),
    private val groqWhisper: GroqWhisper = GroqWhisper(apiKeyProvider = { securePreferences.groqApiKey }),
    private val classifier: NoteClassifier = NoteClassifier(GroqLlmClient(apiKeyProvider = { securePreferences.groqApiKey })),
    private val reportSynthesizer: ReportSynthesizer = ReportSynthesizer(GroqLlmClient(apiKeyProvider = { securePreferences.groqApiKey }), templateSchema),
    private val exporter: GoogleDocsExporter = GoogleDocsExporter()
) : ViewModel() {

    val audioPlayer = AudioPlayer()

    val currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val templates: StateFlow<List<TemplateItemEntity>> = templateRepository.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeTemplate: StateFlow<TemplateItemEntity?> = templateRepository.getActiveTemplate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

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

    private val _authConsentIntent = MutableStateFlow<Intent?>(null)
    val authConsentIntent: StateFlow<Intent?> = _authConsentIntent.asStateFlow()

    fun clearAuthConsentIntent() {
        _authConsentIntent.value = null
    }

    private val _currentSttStatus = MutableStateFlow<CurrentSttStatus>(CurrentSttStatus.Idle)
    val currentSttStatus: StateFlow<CurrentSttStatus> = _currentSttStatus.asStateFlow()

    private val _lastCreatedNoteId = MutableStateFlow<Long?>(null)
    val lastCreatedNoteId: StateFlow<Long?> = _lastCreatedNoteId.asStateFlow()

    private val formSetupService by lazy {
        FormSetupService(llmClient = GroqLlmClient(apiKeyProvider = { securePreferences.groqApiKey }))
    }

    private val _formSetupAnalysis = MutableStateFlow<FormSetupAnalysis?>(null)
    val formSetupAnalysis: StateFlow<FormSetupAnalysis?> = _formSetupAnalysis.asStateFlow()

    private val _isFormSettingUp = MutableStateFlow(false)
    val isFormSettingUp: StateFlow<Boolean> = _isFormSettingUp.asStateFlow()

    private val _formSetupError = MutableStateFlow<String?>(null)
    val formSetupError: StateFlow<String?> = _formSetupError.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(securePreferences.isDeveloperModeEnabled)
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    init {
        // Đồng bộ trạng thái STT của bản ghi mới nhất từ Room database (hỗ trợ WorkManager chạy ngầm)
        viewModelScope.launch {
            activeNotes.collect { notes ->
                val lastId = _lastCreatedNoteId.value ?: return@collect
                val note = notes.find { it.id == lastId } ?: return@collect
                when {
                    note.error != null -> {
                        _currentSttStatus.value = CurrentSttStatus.Error(note.error)
                    }
                    !note.text.isNullOrBlank() -> {
                        _currentSttStatus.value = CurrentSttStatus.Success(note.text)
                    }
                    note.status == "RECORDED" -> {
                        if (securePreferences.groqApiKey.isNullOrBlank()) {
                            _currentSttStatus.value = CurrentSttStatus.NoApiKey
                        } else {
                            _currentSttStatus.value = CurrentSttStatus.Transcribing
                        }
                    }
                }
            }
        }
    }

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
            if (securePreferences.groqApiKey.isNullOrBlank()) {
                _currentSttStatus.value = CurrentSttStatus.NoApiKey
            } else {
                _currentSttStatus.value = CurrentSttStatus.Transcribing
            }
            SttWorker.enqueue(appContext, noteId, audioFile.absolutePath)
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
            repository.updateNote(note.copy(error = null, status = "RECORDED"))
            _lastCreatedNoteId.value = note.id
            if (securePreferences.groqApiKey.isNullOrBlank()) {
                _currentSttStatus.value = CurrentSttStatus.NoApiKey
            } else {
                _currentSttStatus.value = CurrentSttStatus.Transcribing
            }
            SttWorker.enqueue(appContext, note.id, note.audioPath)
        }
    }

    fun reclassifyNote(note: NoteEntity) {
        val text = note.text
        if (text.isNullOrBlank()) return
        viewModelScope.launch {
            repository.updateNote(note.copy(status = "TRANSCRIBED"))
            ClassifyWorker.enqueue(appContext, note.id, text)
        }
    }

    fun updateNoteText(noteId: Long, newText: String) {
        viewModelScope.launch {
            repository.updateText(noteId, newText)
            ClassifyWorker.enqueue(appContext, noteId, newText)
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
                val currentTemplate = templateRepository.getActiveTemplateSync()
                val currentSchema = if (currentTemplate != null) {
                    try {
                        TemplateSchema.fromJson(currentTemplate.schemaJson)
                    } catch (_: Exception) {
                        templateSchema
                    }
                } else {
                    templateSchema
                }
                val promptToUse = currentTemplate?.systemPrompt

                val dynamicSynthesizer = ReportSynthesizer(
                    llmClient = GroqLlmClient(apiKeyProvider = { securePreferences.groqApiKey }),
                    schema = currentSchema,
                    customSystemPrompt = promptToUse
                )

                val result = dynamicSynthesizer.synthesizeReport(currentDate, notes)
                if (result.isSuccess) {
                    val json = result.getOrThrow()
                    val reportEntity = DailyReportEntity(
                        date = currentDate,
                        templateId = currentSchema.id,
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

        // Tự động xếp hàng qua ExportWorker để bảo đảm tự động retry khi mạng chập chờn
        ExportWorker.enqueue(context, currentDate)

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
                    val ex = tokenResult.exceptionOrNull()
                    if (ex is UserRecoverableAuthException) {
                        _isExporting.value = false
                        _authConsentIntent.value = ex.intent
                        return@launch
                    }
                    _exportError.value = "Không thể lấy quyền truy cập Google: ${ex?.message}"
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

    fun setActiveTemplate(id: String) {
        viewModelScope.launch {
            templateRepository.setActiveTemplate(id)
        }
    }

    fun saveCustomTemplate(template: TemplateItemEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            templateRepository.saveTemplate(template)
            onComplete()
        }
    }

    fun duplicateTemplate(template: TemplateItemEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val duplicated = template.copy(
                id = "custom_" + System.currentTimeMillis(),
                name = "${template.name} (Bản sao)",
                isBuiltIn = false,
                isActive = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            templateRepository.saveTemplate(duplicated)
            onComplete()
        }
    }

    fun deleteTemplate(id: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(id)
            onComplete()
        }
    }

    fun startFormSetup(context: Context, docUrlOrId: String) {
        val docId = PlaceholderMapper.extractDocId(docUrlOrId)
        if (docId.isBlank()) {
            _formSetupError.value = "Link hoặc ID Google Doc không hợp lệ."
            return
        }

        val account = GoogleAuthManager.getLastSignedInAccount(context)
        if (account == null || account.account == null) {
            _formSetupError.value = "Chưa đăng nhập tài khoản Google. Vui lòng bấm 'Đăng nhập Google' trong Cài đặt trước."
            return
        }

        viewModelScope.launch {
            _isFormSettingUp.value = true
            _formSetupError.value = null
            _formSetupAnalysis.value = null

            val tokenResult = GoogleAuthManager.fetchAccessToken(context, account.account!!)
            if (tokenResult.isFailure) {
                val ex = tokenResult.exceptionOrNull()
                if (ex is UserRecoverableAuthException) {
                    _isFormSettingUp.value = false
                    _authConsentIntent.value = ex.intent
                    return@launch
                }
                _formSetupError.value = "Không thể lấy quyền truy cập Google: ${ex?.message}"
                _isFormSettingUp.value = false
                return@launch
            }

            val token = tokenResult.getOrThrow()
            val analysisResult = formSetupService.analyzeForm(docId, token)

            _isFormSettingUp.value = false
            if (analysisResult.isSuccess) {
                _formSetupAnalysis.value = analysisResult.getOrThrow()
            } else {
                _formSetupError.value = analysisResult.exceptionOrNull()?.message ?: "Lỗi phân tích biểu mẫu"
            }
        }
    }

    fun rescanFormSetup(context: Context) {
        val analysis = _formSetupAnalysis.value ?: return
        val account = GoogleAuthManager.getLastSignedInAccount(context)
        if (account == null || account.account == null) {
            _formSetupError.value = "Chưa đăng nhập tài khoản Google."
            return
        }

        viewModelScope.launch {
            _isFormSettingUp.value = true
            _formSetupError.value = null

            val tokenResult = GoogleAuthManager.fetchAccessToken(context, account.account!!)
            if (tokenResult.isFailure) {
                _formSetupError.value = "Không thể lấy quyền truy cập Google"
                _isFormSettingUp.value = false
                return@launch
            }

            val token = tokenResult.getOrThrow()
            val result = formSetupService.rescanWorkingDoc(
                workingDocId = analysis.workingDocId,
                workingTitle = analysis.originalDocTitle,
                accessToken = token
            )

            _isFormSettingUp.value = false
            if (result.isSuccess) {
                _formSetupAnalysis.value = result.getOrThrow()
            } else {
                _formSetupError.value = result.exceptionOrNull()?.message ?: "Lỗi quét lại biểu mẫu"
            }
        }
    }

    fun confirmFormSetup(
        context: Context,
        templateName: String,
        candidates: List<FormCandidate>,
        onSuccess: (TemplateItemEntity) -> Unit
    ) {
        val analysis = _formSetupAnalysis.value ?: return
        val account = GoogleAuthManager.getLastSignedInAccount(context) ?: return

        viewModelScope.launch {
            _isFormSettingUp.value = true
            _formSetupError.value = null

            val tokenResult = GoogleAuthManager.fetchAccessToken(context, account.account!!)
            if (tokenResult.isFailure) {
                _formSetupError.value = "Không thể lấy quyền truy cập Google"
                _isFormSettingUp.value = false
                return@launch
            }

            val token = tokenResult.getOrThrow()
            val result = formSetupService.applyPlaceholdersAndGenerateTemplate(
                workingDocId = analysis.workingDocId,
                templateName = templateName,
                candidates = candidates,
                accessToken = token
            )

            _isFormSettingUp.value = false
            if (result.isSuccess) {
                val template = result.getOrThrow()
                templateRepository.saveTemplate(template)
                templateRepository.setActiveTemplate(template.id)
                securePreferences.docTemplateId = analysis.workingDocId
                _formSetupAnalysis.value = null
                onSuccess(template)
            } else {
                _formSetupError.value = result.exceptionOrNull()?.message ?: "Lỗi chèn placeholder"
            }
        }
    }

    fun setDeveloperMode(enabled: Boolean) {
        securePreferences.isDeveloperModeEnabled = enabled
        _isDeveloperMode.value = enabled
    }

    fun updateDailyReportContent(newContentJson: String) {
        val current = dailyReport.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                contentJson = newContentJson.trim(),
                updatedAt = System.currentTimeMillis()
            )
            dailyReportRepository.saveReport(updated)
        }
    }

    fun updateActiveTemplatePrompt(templateId: String, newPrompt: String) {
        viewModelScope.launch {
            templateRepository.updateTemplatePrompt(templateId, newPrompt)
        }
    }

    fun resetActiveTemplatePrompt(templateId: String) {
        viewModelScope.launch {
            templateRepository.resetTemplatePrompt(templateId)
        }
    }

    fun dismissFormSetup() {
        _formSetupAnalysis.value = null
        _formSetupError.value = null
        _isFormSettingUp.value = false
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}

class MainViewModelFactory(
    private val appContext: Context,
    private val repository: NoteRepository,
    private val dailyInputRepository: DailyInputRepository,
    private val dailyReportRepository: DailyReportRepository,
    private val securePreferences: SecurePreferences,
    private val templateSchema: TemplateSchema,
    private val templateRepository: TemplateRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val db = AppDatabase.getDatabase(appContext)
            val repo = templateRepository ?: DefaultTemplateRepository(appContext, db.templateDao())
            return MainViewModel(appContext, repository, dailyInputRepository, dailyReportRepository, securePreferences, templateSchema, templateRepository = repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
