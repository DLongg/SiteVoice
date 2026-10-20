package com.sitevoice.app.llm

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
    private val model: String = "llama-3.1-8b-instant",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : LlmClient {

    companion object {
        private const val GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
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

        try {
            val rootJson = JSONObject().apply {
                put("model", model)
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
                .url(GROQ_CHAT_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
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

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
