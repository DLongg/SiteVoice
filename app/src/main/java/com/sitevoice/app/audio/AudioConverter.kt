package com.sitevoice.app.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object AudioConverter {

    private const val TAG = "AudioConverter"
    const val TARGET_SAMPLE_RATE = 16000
    const val TARGET_CHANNELS = 1

    /**
     * Tạo 44-byte WAV header chuẩn cho PCM 16-bit.
     */
    fun createWavHeader(
        pcmDataSize: Int,
        sampleRate: Int = TARGET_SAMPLE_RATE,
        channels: Int = TARGET_CHANNELS
    ): ByteArray {
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * channels * 2
        val blockAlign = channels * 2

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // fmt sub-chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size = 16 for PCM
        buffer.putShort(1.toShort()) // AudioFormat 1 = PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(16.toShort()) // BitsPerSample = 16

        // data sub-chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(pcmDataSize)

        return header
    }

    /**
     * Chuyển đổi và nội suy PCM 16-bit sang 16000Hz mono.
     */
    fun resamplePcmTo16kMono(
        inputPcm: ShortArray,
        sourceSampleRate: Int,
        sourceChannels: Int
    ): ShortArray {
        // 1. Chuyển stereo -> mono nếu cần
        val monoPcm = if (sourceChannels > 1) {
            val monoLength = inputPcm.size / sourceChannels
            val mono = ShortArray(monoLength)
            for (i in 0 until monoLength) {
                var sum = 0
                for (ch in 0 until sourceChannels) {
                    sum += inputPcm[i * sourceChannels + ch]
                }
                mono[i] = (sum / sourceChannels).toShort()
            }
            mono
        } else {
            inputPcm
        }

        if (sourceSampleRate == TARGET_SAMPLE_RATE) {
            return monoPcm
        }

        // 2. Nội suy tuyến tính sang 16kHz
        val ratio = sourceSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
        val targetLength = (monoPcm.size / ratio).toInt()
        val output = ShortArray(targetLength)

        for (i in 0 until targetLength) {
            val srcIndex = i * ratio
            val indexFloor = srcIndex.toInt()
            val fraction = srcIndex - indexFloor
            val indexCeil = min(indexFloor + 1, monoPcm.size - 1)

            val s0 = monoPcm[indexFloor].toDouble()
            val s1 = monoPcm[indexCeil].toDouble()
            val interpolated = (s0 + fraction * (s1 - s0)).toInt()

            output[i] = interpolated.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return output
    }

    /**
     * Giải mã file M4A (hoặc các định dạng âm thanh Android hỗ trợ) thành file WAV 16kHz Mono.
     */
    fun decodeToWav16kMono(inputFile: File, outputFile: File): Result<File> {
        if (!inputFile.exists()) {
            return Result.failure(IllegalArgumentException("File đầu vào không tồn tại: ${inputFile.absolutePath}"))
        }

        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null

        return try {
            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var trackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (trackIndex == -1 || format == null) {
                return Result.failure(IllegalStateException("Không tìm thấy track âm thanh trong file"))
            }

            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmOutputStream = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false

            val timeoutUs = 5000L

            while (!isEos) {
                val inputIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        pcmOutputStream.write(chunk)
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                        break
                    }
                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                }
            }

            val rawPcmBytes = pcmOutputStream.toByteArray()
            val shortCount = rawPcmBytes.size / 2
            val shortBuffer = ByteBuffer.wrap(rawPcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val pcmShorts = ShortArray(shortCount)
            shortBuffer.get(pcmShorts)

            // Resample & convert sang 16kHz Mono
            val targetPcmShorts = resamplePcmTo16kMono(pcmShorts, sampleRate, channelCount)

            val finalPcmBytes = ByteArray(targetPcmShorts.size * 2)
            ByteBuffer.wrap(finalPcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(targetPcmShorts)

            // Ghi file WAV hoàn chỉnh
            val wavHeader = createWavHeader(finalPcmBytes.size, TARGET_SAMPLE_RATE, TARGET_CHANNELS)
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                fos.write(wavHeader)
                fos.write(finalPcmBytes)
                fos.flush()
            }

            Log.i(TAG, "Đã chuyển đổi thành công sang WAV 16kHz: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi giải mã âm thanh", e)
            Result.failure(e)
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {}
            try {
                extractor?.release()
            } catch (_: Exception) {}
        }
    }
}
