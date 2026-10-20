package com.sitevoice.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {

    companion object {
        const val KEY_GROQ_API_KEY = "groq_api_key"
        const val KEY_OPENAI_API_KEY = "openai_api_key"
        const val KEY_ANTHROPIC_API_KEY = "anthropic_api_key"
        const val KEY_GEMINI_API_KEY = "gemini_api_key"

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
