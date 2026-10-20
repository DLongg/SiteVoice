package com.sitevoice.app.queue

import android.content.Context
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

        fun createWorkRequest(noteId: Long, audioPath: String): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
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
            val request = createWorkRequest(noteId, audioPath)
            WorkManager.getInstance(context).enqueueUniqueWork(
                "stt_work_$noteId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
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
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "Chưa cài đặt Groq API Key")
            noteDao.update(note.copy(error = "Chưa cài đặt API Key trong Cài đặt"))
            return Result.failure()
        }

        val whisper = GroqWhisper(apiKeyProvider = { apiKey })
        Log.i(TAG, "Bắt đầu gọi STT cho note $noteId (attempt: $runAttemptCount)")

        val sttResult = whisper.transcribe(audioFile)
        if (sttResult.isSuccess) {
            val text = sttResult.getOrThrow()
            Log.i(TAG, "STT thành công cho note $noteId: $text")
            noteDao.update(
                note.copy(
                    text = text,
                    sttSource = "groq",
                    status = "TRANSCRIBED",
                    error = null
                )
            )

            // Tự động xếp hàng phân loại LLM ngay sau khi STT thành công
            ClassifyWorker.enqueue(applicationContext, noteId, text)
            return Result.success()
        } else {
            val errorMsg = sttResult.exceptionOrNull()?.message ?: "Lỗi chuyển đổi giọng nói"
            Log.e(TAG, "STT thất bại cho note $noteId: $errorMsg")

            if (runAttemptCount < 3) {
                // Cho phép WorkManager thử lại với exponential backoff khi có mạng
                return Result.retry()
            } else {
                noteDao.update(note.copy(error = errorMsg))
                return Result.failure()
            }
        }
    }
}
