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
        assertTrue(models.size >= 3)

        val tiny = models.find { it.id == "tiny" }
        assertNotNull(tiny)
        assertEquals("ggml-tiny.bin", tiny!!.fileName)
        assertTrue(tiny.downloadUrl.contains("ggml-tiny.bin"))

        val base = models.find { it.id == "base" }
        assertNotNull(base)
        assertEquals("ggml-base.bin", base!!.fileName)
        assertTrue(base.isRecommended)

        val small = models.find { it.id == "small" }
        assertNotNull(small)
        assertEquals("ggml-small.bin", small!!.fileName)
    }

    @Test
    fun testGetModelInfoFallback() {
        val unknown = WhisperModelManager.getModelInfo("non_existent_model")
        assertEquals("base", unknown.id) // Fallback to recommended base
    }
}
