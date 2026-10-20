package com.sitevoice.app

import com.sitevoice.app.settings.SecurePreferences
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
