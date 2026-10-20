package com.sitevoice.app

import com.sitevoice.app.audio.AudioConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioConverterTest {

    @Test
    fun testWavHeaderCreation() {
        val pcmSize = 32000 // 1 second of 16kHz 16-bit mono PCM
        val header = AudioConverter.createWavHeader(pcmSize, 16000, 1)

        assertEquals(44, header.size)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // "RIFF"
        val riff = ByteArray(4)
        buffer.get(riff)
        assertEquals("RIFF", String(riff))

        val totalDataLen = buffer.getInt()
        assertEquals(pcmSize + 36, totalDataLen)

        // "WAVE"
        val wave = ByteArray(4)
        buffer.get(wave)
        assertEquals("WAVE", String(wave))

        // "fmt "
        val fmt = ByteArray(4)
        buffer.get(fmt)
        assertEquals("fmt ", String(fmt))

        val subchunk1Size = buffer.getInt()
        assertEquals(16, subchunk1Size)

        val audioFormat = buffer.getShort()
        assertEquals(1.toShort(), audioFormat) // PCM

        val channels = buffer.getShort()
        assertEquals(1.toShort(), channels) // Mono

        val sampleRate = buffer.getInt()
        assertEquals(16000, sampleRate)

        val byteRate = buffer.getInt()
        assertEquals(32000, byteRate)

        val blockAlign = buffer.getShort()
        assertEquals(2.toShort(), blockAlign)

        val bitsPerSample = buffer.getShort()
        assertEquals(16.toShort(), bitsPerSample)

        // "data"
        val data = ByteArray(4)
        buffer.get(data)
        assertEquals("data", String(data))

        val dataSize = buffer.getInt()
        assertEquals(pcmSize, dataSize)
    }

    @Test
    fun testResamplePcmPreserves16kMono() {
        val input = ShortArray(1600) { it.toShort() }
        val output = AudioConverter.resamplePcmTo16kMono(input, 16000, 1)

        assertEquals(1600, output.size)
        assertEquals(input[0], output[0])
        assertEquals(input[100], output[100])
    }

    @Test
    fun testResampleStereoToMono() {
        // 4 stereo frames = 8 samples (L, R, L, R...)
        val input = shortArrayOf(100, 200, 300, 400, 500, 600, 700, 800)
        val output = AudioConverter.resamplePcmTo16kMono(input, 16000, 2)

        assertEquals(4, output.size)
        assertEquals(150.toShort(), output[0]) // (100 + 200) / 2
        assertEquals(350.toShort(), output[1]) // (300 + 400) / 2
        assertEquals(550.toShort(), output[2])
        assertEquals(750.toShort(), output[3])
    }

    @Test
    fun testResample48kTo16k() {
        // 48000Hz -> 16000Hz is a 3:1 downsampling ratio
        val input = ShortArray(4800) { 1000.toShort() }
        val output = AudioConverter.resamplePcmTo16kMono(input, 48000, 1)

        assertEquals(1600, output.size)
        assertTrue(output.all { it == 1000.toShort() })
    }
}
