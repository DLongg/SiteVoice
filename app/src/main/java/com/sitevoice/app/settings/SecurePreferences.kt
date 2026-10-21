package com.sitevoice.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sitevoice.app.BuildConfig

class SecurePreferences(context: Context) {

    companion object {
        const val KEY_GROQ_API_KEY = "groq_api_key"
        const val KEY_OPENAI_API_KEY = "openai_api_key"
        const val KEY_ANTHROPIC_API_KEY = "anthropic_api_key"
        const val KEY_GEMINI_API_KEY = "gemini_api_key"
        const val KEY_GEMINI_BASE_URL = "gemini_base_url"
        const val KEY_GEMINI_MODEL = "gemini_model"

        const val KEY_DOC_TEMPLATE_ID = "doc_template_id"
        const val DEFAULT_DOC_TEMPLATE_ID = "1qe5AOo45Q9N1XenU7gsdAQkbbmSZ5guO"

        const val KEY_GOOGLE_WEB_CLIENT_ID = "google_web_client_id"
        const val KEY_GOOGLE_ACCOUNT_EMAIL = "google_account_email"
        const val KEY_GOOGLE_ACCESS_TOKEN = "google_access_token"

        const val KEY_DEVELOPER_MODE = "developer_mode_enabled"

        fun extractDocId(input: String): String {
            val trimmed = input.trim()
            if (trimmed.isBlank()) return DEFAULT_DOC_TEMPLATE_ID
            val regex = "/d/([a-zA-Z0-9_-]+)".toRegex()
            val match = regex.find(trimmed)
            return match?.groupValues?.get(1) ?: trimmed
        }
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "sitevoice_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var groqApiKey: String?
        get() = sharedPreferences.getString(KEY_GROQ_API_KEY, null)
        set(value) = sharedPreferences.edit().putString(KEY_GROQ_API_KEY, value).apply()

    var geminiApiKey: String?
        get() {
            val saved = sharedPreferences.getString(KEY_GEMINI_API_KEY, null)
            if (!saved.isNullOrBlank()) return saved
            return try {
                BuildConfig.DEFAULT_CLASSIFY_API_KEY.ifBlank { null }
            } catch (e: Throwable) {
                null
            }
        }
        set(value) = sharedPreferences.edit().putString(KEY_GEMINI_API_KEY, value?.trim()).apply()

    var geminiBaseUrl: String
        get() {
            val saved = sharedPreferences.getString(KEY_GEMINI_BASE_URL, null)
            if (!saved.isNullOrBlank()) return saved
            return try {
                BuildConfig.DEFAULT_CLASSIFY_BASE_URL.ifBlank { "https://api.vilao.ai/v1" }
            } catch (e: Throwable) {
                "https://api.vilao.ai/v1"
            }
        }
        set(value) = sharedPreferences.edit().putString(KEY_GEMINI_BASE_URL, value?.trim()).apply()

    var geminiModel: String
        get() {
            val saved = sharedPreferences.getString(KEY_GEMINI_MODEL, null)
            if (!saved.isNullOrBlank()) return saved
            return try {
                BuildConfig.DEFAULT_CLASSIFY_MODEL.ifBlank { "acc/gemini-3.8-flash" }
            } catch (e: Throwable) {
                "acc/gemini-3.8-flash"
            }
        }
        set(value) = sharedPreferences.edit().putString(KEY_GEMINI_MODEL, value?.trim()).apply()

    var docTemplateId: String
        get() = sharedPreferences.getString(KEY_DOC_TEMPLATE_ID, null)?.ifBlank { DEFAULT_DOC_TEMPLATE_ID } ?: DEFAULT_DOC_TEMPLATE_ID
        set(value) = sharedPreferences.edit().putString(KEY_DOC_TEMPLATE_ID, extractDocId(value)).apply()

    var googleWebClientId: String?
        get() = sharedPreferences.getString(KEY_GOOGLE_WEB_CLIENT_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_GOOGLE_WEB_CLIENT_ID, value?.trim()).apply()

    var googleAccountEmail: String?
        get() = sharedPreferences.getString(KEY_GOOGLE_ACCOUNT_EMAIL, null)
        set(value) = sharedPreferences.edit().putString(KEY_GOOGLE_ACCOUNT_EMAIL, value?.trim()).apply()

    var googleAccessToken: String?
        get() = sharedPreferences.getString(KEY_GOOGLE_ACCESS_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_GOOGLE_ACCESS_TOKEN, value).apply()

    var isDeveloperModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_DEVELOPER_MODE, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_DEVELOPER_MODE, value).apply()

    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun setString(key: String, value: String?) {
        sharedPreferences.edit().putString(key, value).apply()
    }
}
