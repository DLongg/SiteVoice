package com.sitevoice.app.llm

interface LlmClient {
    suspend fun classifyNote(text: String, candidateLabels: List<String>): Result<String>
    suspend fun summarizeReport(prompt: String): Result<String>
}
