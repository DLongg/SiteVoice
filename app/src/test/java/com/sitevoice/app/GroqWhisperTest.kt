package com.sitevoice.app

import com.sitevoice.app.stt.GroqWhisper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GroqWhisperTest {

    @Test
    fun testGroqWhisperMissingApiKey() {
        runBlocking {
            val whisper = GroqWhisper(apiKeyProvider = { null })
            val tempFile = File.createTempFile("test_audio", ".m4a")
            tempFile.writeText("dummy audio content")

            val result = whisper.transcribe(tempFile)
            assertTrue("Khi chưa có API key phải trả về Failure", result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Chưa cài đặt API Key") == true)

            tempFile.delete()
        }
    }

    @Test
    fun testGroqWhisperMissingFile() {
        runBlocking {
            val whisper = GroqWhisper(apiKeyProvider = { "gsk_dummy_test_key" })
            val nonExistentFile = File("non_existent_file.m4a")

            val result = whisper.transcribe(nonExistentFile)
            assertTrue("Khi file không tồn tại phải trả về Failure", result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("không tồn tại") == true)
        }
    }

    @Test
    fun testIsHallucinationOrSilenceDetection() {
        // Các mẫu hallucination YouTube
        assertTrue(GroqWhisper.isHallucinationOrSilence("Hãy subscribe cho kênh LaLa School nhé!"))
        assertTrue(GroqWhisper.isHallucinationOrSilence("Đăng ký kênh để xem clip tiếp theo"))
        assertTrue(GroqWhisper.isHallucinationOrSilence("Cảm ơn các bạn đã theo dõi video"))
        assertTrue(GroqWhisper.isHallucinationOrSilence("   "))
        assertTrue(GroqWhisper.isHallucinationOrSilence("a"))

        // Văn bản hợp lệ công trường không được coi là hallucination
        org.junit.Assert.assertFalse(GroqWhisper.isHallucinationOrSilence("Hôm nay tổ bê tông đổ 50m3 sàn tầng 2."))
        org.junit.Assert.assertFalse(GroqWhisper.isHallucinationOrSilence("Kỹ sư TVGS kiểm tra nghiệm thu cốt thép móng."))
    }
}
