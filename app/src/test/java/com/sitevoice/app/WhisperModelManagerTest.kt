package com.sitevoice.app

import com.sitevoice.app.stt.WhisperModelManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperModelManagerTest {

    @Test
    fun testAvailableModels() {
        val models = WhisperModelManager.AVAILABLE_MODELS
        assertTrue(models.size >= 4)

        val tiny = models.find { it.id == "tiny" }
        assertNotNull(tiny)
        assertEquals("ggml-tiny.bin", tiny!!.fileName)
        assertTrue(tiny.downloadUrl.contains("ggml-tiny.bin"))

        val base = models.find { it.id == "base" }
        assertNotNull(base)
        assertEquals("ggml-base.bin", base!!.fileName)

        val small = models.find { it.id == "small" }
        assertNotNull(small)
        assertEquals("ggml-small.bin", small!!.fileName)

        val turboQ5 = models.find { it.id == "turbo-q5" }
        assertNotNull(turboQ5)
        assertEquals("ggml-large-v3-turbo-q5_0.bin", turboQ5!!.fileName)
        assertTrue(turboQ5.isRecommended)
    }

    @Test
    fun testGetModelInfoFallback() {
        val unknown = WhisperModelManager.getModelInfo("non_existent_model")
        assertEquals("turbo-q5", unknown.id) // Fallback to recommended turbo-q5
    }
}
