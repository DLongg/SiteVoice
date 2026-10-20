package com.sitevoice.app.llm

data class LlmMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

interface LlmClient {
    suspend fun chat(
        messages: List<LlmMessage>,
        temperature: Float = 0.0f,
        responseJson: Boolean = false
    ): Result<String>
}
