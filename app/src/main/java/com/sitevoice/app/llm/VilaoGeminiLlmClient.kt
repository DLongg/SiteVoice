package com.sitevoice.app.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client gọi mô hình LLM (Gemini 3.8 Flash) qua cổng API tương thích OpenAI của Vilao AI (https://api.vilao.ai/v1).
 * Dùng chuyên biệt cho tác vụ Đọc & Phân loại ghi chú (và tổng hợp báo cáo).
 */
class VilaoGeminiLlmClient(
    private val apiKeyProvider: () -> String?,
    private val baseUrlProvider: () -> String = { DEFAULT_BASE_URL },
    private val modelProvider: () -> String = { DEFAULT_MODEL },
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : LlmClient {

    companion object {
        private const val TAG = "VilaoGeminiLLM"
        const val DEFAULT_BASE_URL = "https://api.vilao.ai/v1"
        const val DEFAULT_MODEL = "acc/gemini-3.8-flash"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun chat(
        messages: List<LlmMessage>,
        temperature: Float,
        responseJson: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()?.trim()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Chưa cấu hình API Key Phân loại (Gemini)"))
        }

        val rawBaseUrl = baseUrlProvider().trim().trimEnd('/')
        val endpoint = if (rawBaseUrl.endsWith("/chat/completions")) {
            rawBaseUrl
        } else {
            "$rawBaseUrl/chat/completions"
        }
        val modelName = modelProvider().trim().ifBlank { DEFAULT_MODEL }

        try {
            Log.d(TAG, "Đang gọi Gemini Phân loại ($modelName) tại endpoint: $endpoint")

            val rootJson = JSONObject().apply {
                put("model", modelName)
                put("temperature", temperature.toDouble())

                if (responseJson) {
                    put("response_format", JSONObject().apply {
                        put("type", "json_object")
                    })
                }

                val messagesArray = JSONArray()
                for (msg in messages) {
                    val msgObj = JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    }
                    messagesArray.put(msgObj)
                }
                put("messages", messagesArray)
            }

            val requestBody = rootJson.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "SiteVoice/1.0")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody ?: "")
                    errJson.optJSONObject("error")?.optString("message")
                        ?: "Mã lỗi HTTP: ${response.code}"
                } catch (e: Exception) {
                    "Mã lỗi HTTP: ${response.code}"
                }
                Log.w(TAG, "Gemini $modelName thất bại: $errorMsg (code: ${response.code})")
                return@withContext Result.failure(IOException("Lỗi Gemini API ($modelName): $errorMsg"))
            }

            if (responseBody.isNullOrBlank()) {
                return@withContext Result.failure(IOException("Phản hồi từ Gemini API trống"))
            }

            val resJson = JSONObject(responseBody)
            val choices = resJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(IOException("Không tìm thấy kết quả từ Gemini API"))
            }

            val firstChoice = choices.getJSONObject(0)
            val messageObj = firstChoice.optJSONObject("message")
            val content = messageObj?.optString("content")?.trim().orEmpty()

            Log.i(TAG, "Gemini LLM thành công với model $modelName. Output: $content")
            Result.success(content)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gọi Gemini Phân loại: ${e.message}", e)
            Result.failure(e)
        }
    }
}
