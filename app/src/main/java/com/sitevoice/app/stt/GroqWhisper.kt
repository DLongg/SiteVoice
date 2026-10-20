package com.sitevoice.app.stt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GroqWhisper(
    private val apiKeyProvider: () -> String?,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : SpeechToText {

    companion object {
        private const val GROQ_TRANSCRIPTION_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
        private const val DEFAULT_MODEL = "whisper-large-v3-turbo"

        // Từ điển thuật ngữ công trường theo docs/09-thuat-ngu.md
        const val DEFAULT_INITIAL_PROMPT =
            "Nhật ký thi công công trường xây dựng, CĐT, TVGS, BCH, ATLĐ, VSMT, " +
            "bê tông, cốt thép, cốp pha, dầm sàn, giàn giáo, nghiệm thu, trạm trộn, " +
            "máy đục, máy cắt rãnh, máy khoan bê tông, máy mài góc, máy laser."
    }

    override suspend fun transcribe(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()?.trim()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Chưa cài đặt API Key Groq. Vui lòng cấu hình trong Cài đặt.")
            )
        }

        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalArgumentException("File âm thanh không tồn tại hoặc rỗng: ${audioFile.name}")
            )
        }

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", DEFAULT_MODEL)
                .addFormDataPart("language", "vi")
                .addFormDataPart("response_format", "json")
                .addFormDataPart("temperature", "0.0")
                .addFormDataPart("prompt", DEFAULT_INITIAL_PROMPT)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(GROQ_TRANSCRIPTION_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    401 -> "API Key Groq không hợp lệ hoặc đã hết hạn (401)."
                    429 -> "Đã vượt quá giới hạn lượt gọi Groq API (Rate limit 429)."
                    else -> "Lỗi từ Groq API (${response.code}): $responseBody"
                }
                return@withContext Result.failure(IOException(errorMsg))
            }

            val json = JSONObject(responseBody)
            val text = json.optString("text", "").trim()
            if (text.isEmpty()) {
                Result.failure(IOException("Không nhận diện được giọng nói trong bản ghi."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
