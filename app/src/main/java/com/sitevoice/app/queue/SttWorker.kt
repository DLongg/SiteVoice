package com.sitevoice.app.queue

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sitevoice.app.data.AppDatabase
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.stt.GroqWhisper
import com.sitevoice.app.stt.WhisperModelManager
import com.sitevoice.app.stt.WhisperOfflineStt
import java.io.File
import java.util.concurrent.TimeUnit

class SttWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SttWorker"
        const val KEY_NOTE_ID = "note_id"
        const val KEY_AUDIO_PATH = "audio_path"

        fun createWorkRequest(noteId: Long, audioPath: String, requireNetwork: Boolean = true): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (requireNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
                .build()

            val inputData = Data.Builder()
                .putLong(KEY_NOTE_ID, noteId)
                .putString(KEY_AUDIO_PATH, audioPath)
                .build()

            return OneTimeWorkRequestBuilder<SttWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.SECONDS
                )
                .addTag(TAG)
                .addTag("note_$noteId")
                .build()
        }

        fun enqueue(context: Context, noteId: Long, audioPath: String) {
            val securePreferences = SecurePreferences(context)
            val isOfflineEnabled = securePreferences.isOfflineSttEnabled
            val offlineModelId = securePreferences.selectedOfflineModelId
            val isModelReady = isOfflineEnabled && WhisperModelManager.isModelDownloaded(context, offlineModelId)

            // Nếu đã tải và bật model offline: không bắt buộc phải có mạng để chạy STT
            val requireNetwork = !isModelReady
            val request = createWorkRequest(noteId, audioPath, requireNetwork)

            WorkManager.getInstance(context).enqueueUniqueWork(
                "stt_work_$noteId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        val audioPath = inputData.getString(KEY_AUDIO_PATH)

        if (noteId == -1L || audioPath.isNullOrBlank()) {
            Log.e(TAG, "Dữ liệu đầu vào không hợp lệ: noteId=$noteId, audioPath=$audioPath")
            return Result.failure()
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val noteDao = database.noteDao()
        val note = noteDao.getNoteById(noteId) ?: run {
            Log.e(TAG, "Không tìm thấy ghi chú id=$noteId trong cơ sở dữ liệu")
            return Result.failure()
        }

        val audioFile = File(audioPath)
        if (!audioFile.exists()) {
            Log.e(TAG, "File âm thanh không tồn tại tại: $audioPath")
            noteDao.update(note.copy(error = "File âm thanh không tồn tại"))
            return Result.failure()
        }

        val securePreferences = SecurePreferences(applicationContext)
        val apiKey = securePreferences.groqApiKey
        val isOfflineEnabled = securePreferences.isOfflineSttEnabled
        val offlineModelId = securePreferences.selectedOfflineModelId
        val isModelReady = isOfflineEnabled && WhisperModelManager.isModelDownloaded(applicationContext, offlineModelId)
        val hasNetwork = isNetworkAvailable(applicationContext)

        Log.i(TAG, "Bắt đầu STT cho note $noteId (attempt: $runAttemptCount, hasNetwork: $hasNetwork, isModelReady: $isModelReady)")

        // 1. Nếu có mạng và có API key: Ưu tiên Groq Whisper online
        if (hasNetwork && !apiKey.isNullOrBlank()) {
            val whisper = GroqWhisper(apiKeyProvider = { apiKey })
            val sttResult = whisper.transcribe(audioFile)
            if (sttResult.isSuccess) {
                val text = sttResult.getOrThrow()
                Log.i(TAG, "Groq STT thành công cho note $noteId: $text")
                noteDao.update(
                    note.copy(
                        text = text,
                        sttSource = "groq",
                        status = "TRANSCRIBED",
                        error = null
                    )
                )
                ClassifyWorker.enqueue(applicationContext, noteId, text)
                return Result.success()
            } else {
                Log.w(TAG, "Groq STT thất bại: ${sttResult.exceptionOrNull()?.message}")
                // Nếu có model offline sẵn sàng, fallback sang offline
                if (isModelReady) {
                    Log.i(TAG, "Chuyển hướng fallback sang Whisper offline...")
                    return runOfflineStt(noteId, audioFile, offlineModelId, noteDao, note)
                }
            }
        }

        // 2. Nếu không có mạng (hoặc chưa cài API key) nhưng có model offline: Chạy whisper.cpp on-device
        if (isModelReady) {
            return runOfflineStt(noteId, audioFile, offlineModelId, noteDao, note)
        }

        // 3. Nếu chưa cài API Key và không có model offline
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "Chưa cài đặt Groq API Key và chưa có model Whisper offline")
            noteDao.update(note.copy(error = "Chưa cài đặt API Key hoặc chưa tải model offline"))
            return Result.failure()
        }

        // 4. Nếu không có mạng: Chờ mạng thử lại với exponential backoff
        Log.i(TAG, "Không có mạng và chưa có model offline, chờ kết nối mạng để thử lại...")
        return if (runAttemptCount < 5) Result.retry() else {
            noteDao.update(note.copy(error = "Không có kết nối mạng để nhận diện giọng nói"))
            Result.failure()
        }
    }

    private suspend fun runOfflineStt(
        noteId: Long,
        audioFile: File,
        offlineModelId: String,
        noteDao: com.sitevoice.app.data.NoteDao,
        note: com.sitevoice.app.data.NoteEntity
    ): Result {
        val offlineStt = WhisperOfflineStt(
            context = applicationContext,
            modelFileProvider = { WhisperModelManager.getModelFile(applicationContext, offlineModelId) }
        )

        val result = offlineStt.transcribe(audioFile)
        return if (result.isSuccess) {
            val text = result.getOrThrow()
            Log.i(TAG, "Whisper offline STT thành công cho note $noteId: $text")
            noteDao.update(
                note.copy(
                    text = text,
                    sttSource = "whisper_offline",
                    status = "TRANSCRIBED",
                    error = null
                )
            )
            ClassifyWorker.enqueue(applicationContext, noteId, text)
            Result.success()
        } else {
            val err = result.exceptionOrNull()?.message ?: "Lỗi nhận diện whisper offline"
            Log.e(TAG, "Whisper offline STT thất bại cho note $noteId: $err")
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                noteDao.update(note.copy(error = err))
                Result.failure()
            }
        }
    }
}
