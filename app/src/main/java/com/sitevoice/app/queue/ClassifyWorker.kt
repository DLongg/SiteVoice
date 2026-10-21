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
import com.sitevoice.app.llm.GroqLlmClient
import com.sitevoice.app.llm.HybridClassificationPipeline
import com.sitevoice.app.llm.NoteClassifier
import com.sitevoice.app.settings.SecurePreferences
import java.util.concurrent.TimeUnit

class ClassifyWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ClassifyWorker"
        const val KEY_NOTE_ID = "note_id"
        const val KEY_TEXT = "note_text"

        fun createWorkRequest(noteId: Long, text: String): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putLong(KEY_NOTE_ID, noteId)
                .putString(KEY_TEXT, text)
                .build()

            return OneTimeWorkRequestBuilder<ClassifyWorker>()
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

        fun enqueue(context: Context, noteId: Long, text: String) {
            val request = createWorkRequest(noteId, text)
            WorkManager.getInstance(context).enqueueUniqueWork(
                "classify_work_$noteId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        val text = inputData.getString(KEY_TEXT)

        if (noteId == -1L || text.isNullOrBlank()) {
            Log.e(TAG, "Dữ liệu đầu vào không hợp lệ: noteId=$noteId, text=$text")
            return Result.failure()
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val noteDao = database.noteDao()
        val note = noteDao.getNoteById(noteId) ?: run {
            Log.e(TAG, "Không tìm thấy ghi chú id=$noteId trong cơ sở dữ liệu")
            return Result.failure()
        }

        val securePreferences = SecurePreferences(applicationContext)
        val hasClassificationKey = !securePreferences.geminiApiKey.isNullOrBlank() || !securePreferences.groqApiKey.isNullOrBlank()

        val classifier = if (hasClassificationKey) {
            NoteClassifier(com.sitevoice.app.llm.LlmFactory.createClassificationClient(securePreferences))
        } else {
            null
        }

        val pipeline = HybridClassificationPipeline(classifier)
        val previousNote = noteDao.getPreviousNoteSync(note.date, note.createdAt)

        Log.i(TAG, "Bắt đầu phân loại note $noteId qua Hybrid Pipeline: $text")

        val result = pipeline.classify(
            id = noteId,
            rawText = text,
            userLabel = if (note.labelSource == "user") note.label else null,
            previousNoteLabel = previousNote?.label
        )

        val updatedNote = note.copy(
            label = result.label,
            labelSource = if (note.labelSource == "user") "user" else "auto",
            status = "LABELED"
        )
        noteDao.update(updatedNote)
        Log.i(TAG, "Phân loại thành công note $noteId -> label: ${result.label} (confidence: ${result.confidence})")

        return Result.success()
    }
}
