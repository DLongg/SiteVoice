package com.sitevoice.app.llm

import com.sitevoice.app.settings.SecurePreferences

object LlmFactory {
    /**
     * Tạo LlmClient dùng cho phân loại ghi chú và tổng hợp báo cáo.
     * Ưu tiên dùng Gemini API phân loại (Vilao AI / acc/gemini-3.8-flash).
     * Nếu chưa cấu hình, fallback sang Groq API.
     */
    fun createClassificationClient(securePreferences: SecurePreferences): LlmClient {
        val geminiKey = securePreferences.geminiApiKey
        return if (!geminiKey.isNullOrBlank()) {
            VilaoGeminiLlmClient(
                apiKeyProvider = { securePreferences.geminiApiKey },
                baseUrlProvider = { securePreferences.geminiBaseUrl },
                modelProvider = { securePreferences.geminiModel }
            )
        } else {
            GroqLlmClient(apiKeyProvider = { securePreferences.groqApiKey })
        }
    }
}
