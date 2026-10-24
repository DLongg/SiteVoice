package com.sitevoice.app

import android.content.SharedPreferences
import com.sitevoice.app.settings.SecurePreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeSharedPreferences : SharedPreferences {
    val data = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        (data[key] as? String) ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        (data[key] as? Int) ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        (data[key] as? Long) ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        (data[key] as? Float) ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        (data[key] as? Boolean) ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val parent: FakeSharedPreferences) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private val toRemove = mutableSetOf<String>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) toRemove.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) parent.data.clear()
            toRemove.forEach { parent.data.remove(it) }
            parent.data.putAll(temp)
        }
    }
}

class SecurePreferencesTest {

    @Test
    fun testExtractDocIdFromFullUrl() {
        val url = "https://docs.google.com/document/d/1qe5AOo45Q9N1XenU7gsdAQkbbmSZ5guO/edit?usp=sharing"
        val id = SecurePreferences.extractDocId(url)
        assertEquals("1qe5AOo45Q9N1XenU7gsdAQkbbmSZ5guO", id)
    }

    @Test
    fun testExtractDocIdFromRawId() {
        val raw = "1qe5AOo45Q9N1XenU7gsdAQkbbmSZ5guO"
        val id = SecurePreferences.extractDocId(raw)
        assertEquals("1qe5AOo45Q9N1XenU7gsdAQkbbmSZ5guO", id)
    }

    @Test
    fun testExtractDocIdFromBlank() {
        val blank = "   "
        val id = SecurePreferences.extractDocId(blank)
        assertEquals(SecurePreferences.DEFAULT_DOC_TEMPLATE_ID, id)
    }

    @Test
    fun testLlmTemperatureDefaultValue() {
        val fakePrefs = FakeSharedPreferences()
        val securePrefs = SecurePreferences(fakePrefs)

        assertEquals(SecurePreferences.DEFAULT_LLM_TEMPERATURE, securePrefs.llmTemperature, 0.0001f)
        assertEquals(0.2f, securePrefs.llmTemperature, 0.0001f)
    }

    @Test
    fun testLlmTemperatureSetAndGetWithinRange() {
        val fakePrefs = FakeSharedPreferences()
        val securePrefs = SecurePreferences(fakePrefs)

        securePrefs.llmTemperature = 0.5f
        assertEquals(0.5f, securePrefs.llmTemperature, 0.0001f)
        assertEquals(0.5f, fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] as Float, 0.0001f)

        securePrefs.llmTemperature = 0.0f
        assertEquals(0.0f, securePrefs.llmTemperature, 0.0001f)

        securePrefs.llmTemperature = 0.8f
        assertEquals(0.8f, securePrefs.llmTemperature, 0.0001f)

        securePrefs.llmTemperature = 0.35f
        assertEquals(0.35f, securePrefs.llmTemperature, 0.0001f)
    }

    @Test
    fun testLlmTemperatureClamping() {
        val fakePrefs = FakeSharedPreferences()
        val securePrefs = SecurePreferences(fakePrefs)

        // Test clamping below 0.0f
        securePrefs.llmTemperature = -0.5f
        assertEquals(0.0f, securePrefs.llmTemperature, 0.0001f)
        assertEquals(0.0f, fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] as Float, 0.0001f)

        securePrefs.llmTemperature = -100.0f
        assertEquals(0.0f, securePrefs.llmTemperature, 0.0001f)

        // Test clamping above 0.8f
        securePrefs.llmTemperature = 1.0f
        assertEquals(0.8f, securePrefs.llmTemperature, 0.0001f)
        assertEquals(0.8f, fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] as Float, 0.0001f)

        securePrefs.llmTemperature = 0.85f
        assertEquals(0.8f, securePrefs.llmTemperature, 0.0001f)

        securePrefs.llmTemperature = 99.0f
        assertEquals(0.8f, securePrefs.llmTemperature, 0.0001f)

        // Test if underlying SharedPreferences contains an out-of-range value directly
        fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] = 1.5f
        assertEquals(0.8f, securePrefs.llmTemperature, 0.0001f)

        fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] = -0.4f
        assertEquals(0.0f, securePrefs.llmTemperature, 0.0001f)
    }
}
