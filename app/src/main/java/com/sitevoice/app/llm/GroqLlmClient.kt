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

class GroqLlmClient(
    private val apiKeyProvider: () -> String?,
    private val preferredModel: String = "llama-3.3-70b-versatile",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : LlmClient {

    companion object {
        private const val TAG = "SiteVoiceLLM"
        private const val GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val FALLBACK_MODELS = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "qwen/qwen3.8-27b",
            "openai/gpt-oss-20b"
        )
    }

    override suspend fun chat(
        messages: List<LlmMessage>,
        temperature: Float,
        responseJson: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Chưa cấu hình API Key Groq"))
        }

        // Danh sách các model để thử, ưu tiên preferredModel
        val modelsToTry = linkedSetOf(preferredModel).apply { addAll(FALLBACK_MODELS) }.toList()

        var lastError: Exception? = null

        for (modelName in modelsToTry) {
            try {
                Log.d(TAG, "Đang gọi Groq LLM với model: $modelName")
                val rootJson = JSONObject().apply {
                    put("model", modelName)
                    put("temperature", temperature.toDouble())
                    put("max_tokens", 800)

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
                    .url(GROQ_CHAT_URL)
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
                    Log.w(TAG, "Model $modelName thất bại: $errorMsg (code: ${response.code})")

                    // Nếu là 404 (model not found), 429 (vượt limit token/rate limit) hoặc lỗi 5xx, thử model tiếp theo trong danh sách
                    if (response.code == 404 || response.code == 429 || response.code >= 500) {
                        lastError = IOException("Model $modelName (${response.code}): $errorMsg")
                        continue
                    }
                    return@withContext Result.failure(IOException("Lỗi Groq LLM API: $errorMsg"))
                }

                if (responseBody.isNullOrBlank()) {
                    return@withContext Result.failure(IOException("Phản hồi từ Groq LLM API trống"))
                }

                val resJson = JSONObject(responseBody)
                val choices = resJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(IOException("Không tìm thấy kết quả trong phản hồi"))
                }

                val firstChoice = choices.getJSONObject(0)
                val messageObj = firstChoice.optJSONObject("message")
                val content = messageObj?.optString("content")?.trim().orEmpty()

                Log.i(TAG, "Groq LLM thành công với model $modelName. Output: $content")
                return@withContext Result.success(content)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi gọi model $modelName: ${e.message}", e)
                lastError = e
            }
        }

        Result.failure(lastError ?: IOException("Không thể kết nối đến bất kỳ model Groq nào"))
    }
}
