package com.sitevoice.app.stt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class WhisperModelInfo(
    val id: String,
    val name: String,
    val fileName: String,
    val downloadUrl: String,
    val approximateSizeBytes: Long,
    val isRecommended: Boolean = false
)

object WhisperModelManager {

    private const val TAG = "WhisperModelManager"

    val AVAILABLE_MODELS = listOf(
        WhisperModelInfo(
            id = "tiny",
            name = "Whisper Tiny",
            fileName = "ggml-tiny.bin",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
            approximateSizeBytes = 75 * 1024 * 1024L, // ~75 MB
            isRecommended = false
        ),
        WhisperModelInfo(
            id = "base",
            name = "Whisper Base",
            fileName = "ggml-base.bin",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
            approximateSizeBytes = 142 * 1024 * 1024L, // ~142 MB
            isRecommended = false
        ),
        WhisperModelInfo(
            id = "small",
            name = "Whisper Small",
            fileName = "ggml-small.bin",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
            approximateSizeBytes = 466 * 1024 * 1024L, // ~466 MB
            isRecommended = false
        ),
        WhisperModelInfo(
            id = "turbo-q5",
            name = "Whisper Large-v3-Turbo (Q5)",
            fileName = "ggml-large-v3-turbo-q5_0.bin",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo-q5_0.bin",
            approximateSizeBytes = 574041195L, // ~547 MB (Chất lượng tiếng Việt cao nhất)
            isRecommended = true
        )
    )

    fun getModelInfo(modelId: String): WhisperModelInfo {
        return AVAILABLE_MODELS.find { it.id == modelId }
            ?: AVAILABLE_MODELS.find { it.isRecommended }
            ?: AVAILABLE_MODELS[1]
    }

    fun getModelsDir(context: Context): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getModelFile(context: Context, modelId: String): File {
        val info = getModelInfo(modelId)
        return File(getModelsDir(context), info.fileName)
    }

    fun isModelDownloaded(context: Context, modelId: String): Boolean {
        val file = getModelFile(context, modelId)
        // File phải tồn tại và kích thước tương đối hợp lệ (> 10MB)
        return file.exists() && file.length() > 10 * 1024 * 1024L
    }

    fun getModelSizeFormatted(context: Context, modelId: String): String {
        val file = getModelFile(context, modelId)
        if (!file.exists()) return "Chưa tải"
        val mb = file.length().toDouble() / (1024.0 * 1024.0)
        return "%.1f MB".format(mb)
    }

    fun deleteModel(context: Context, modelId: String): Boolean {
        val file = getModelFile(context, modelId)
        return if (file.exists()) {
            val deleted = file.delete()
            Log.i(TAG, "Đã xóa model $modelId: $deleted")
            deleted
        } else {
            false
        }
    }

    /**
     * Tải mô hình Whisper từ HuggingFace với báo cáo tiến trình.
     */
    suspend fun downloadModel(
        context: Context,
        modelId: String,
        client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build(),
        onProgress: (progressPercent: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val info = getModelInfo(modelId)
        val targetFile = getModelFile(context, modelId)
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.download")

        try {
            Log.i(TAG, "Bắt đầu tải model: ${info.name} từ ${info.downloadUrl}")
            val request = Request.Builder()
                .url(info.downloadUrl)
                .header("User-Agent", "SiteVoice/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Lỗi tải model: HTTP ${response.code} ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Phản hồi rỗng từ máy chủ"))
            val totalBytes = if (body.contentLength() > 0) body.contentLength() else info.approximateSizeBytes

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesCopied = 0L
                    var lastPercent = -1

                    var read = input.read(buffer)
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read

                        val percent = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent.coerceIn(0, 100))
                        }
                        read = input.read(buffer)
                    }
                    output.flush()
                }
            }

            if (tempFile.renameTo(targetFile)) {
                Log.i(TAG, "Tải model thành công: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                Result.success(targetFile)
            } else {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
                Result.success(targetFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong quá trình tải model", e)
            if (tempFile.exists()) tempFile.delete()
            Result.failure(e)
        }
    }
}
