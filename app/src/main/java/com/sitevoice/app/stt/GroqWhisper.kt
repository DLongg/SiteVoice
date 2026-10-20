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

        // Danh sách các mẫu hallucination phổ biến của Whisper khi bản ghi im lặng hoặc chỉ có tiếng ồn
        val KNOWN_HALLUCINATIONS = listOf(
            "subscribe",
            "sub kênh",
            "đăng ký kênh",
            "đăng kí kênh",
            "like và share",
            "like và đăng ký",
            "lala school",
            "lala",
            "nhấn chuông",
            "bấm chuông",
            "cảm ơn các bạn đã theo dõi",
            "cảm ơn các bạn đã xem",
            "cảm ơn quý vị",
            "chúc các bạn nghe nhạc",
            "hẹn gặp lại các bạn",
            "hẹn gặp lại quý vị",
            "tạm biệt các bạn",
            "phụ đề bởi",
            "bản dịch bởi",
            "trans by",
            "sub by",
            "video tiếp theo",
            "clip tiếp theo",
            "thank you for watching",
            "see you next time"
        )

        fun isHallucinationOrSilence(text: String): Boolean {
            val trimmed = text.trim()
            if (trimmed.length < 3) return true

            val lower = trimmed.lowercase()

            // 1. Khớp với các câu hallucination đặc trưng
            for (phrase in KNOWN_HALLUCINATIONS) {
                if (lower.contains(phrase)) return true
            }

            // 2. Kiểm tra nếu kết quả chỉ là sự nhại lại danh sách từ khóa trong initial prompt
            // (khi không nghe thấy gì thực tế, Whisper thường nhại lại prompt)
            val promptWords = listOf("cđt", "tvgs", "bch", "atlđ", "vsmt", "bê tông", "cốt thép", "cốp pha", "dầm sàn", "giàn giáo", "nghiệm thu", "trạm trộn")
            val wordsInText = lower.split(Regex("[\\s,.;:!?]+")).filter { it.isNotBlank() }
            if (wordsInText.isNotEmpty()) {
                val matchesPromptOnly = wordsInText.all { word ->
                    promptWords.any { it.contains(word) } || word == "nhật" || word == "ký" || word == "thi" || word == "công" || word == "trường" || word == "dựng"
                }
                if (matchesPromptOnly && wordsInText.size <= 8) {
                    return true
                }
            }

            return false
        }
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

            if (text.isEmpty() || isHallucinationOrSilence(text)) {
                Result.failure(IOException("Không phát hiện giọng nói trong bản ghi (chỉ có tiếng ồn hoặc im lặng)."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
