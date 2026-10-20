package com.sitevoice.app.stt

import android.content.Context
import android.util.Log
import com.sitevoice.app.audio.AudioConverter
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import dev.ffmpegkit.whisper.WhisperModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WhisperOfflineStt(
    private val context: Context,
    private val modelFileProvider: () -> File?,
    private val audioConverter: (File, File) -> Result<File> = { input, output ->
        AudioConverter.decodeToWav16kMono(input, output)
    }
) : SpeechToText {

    companion object {
        private const val TAG = "WhisperOfflineStt"
    }

    override suspend fun transcribe(audioFile: File): Result<String> {
        val modelFile = modelFileProvider()
        if (modelFile == null || !modelFile.exists()) {
            return Result.failure(
                IllegalStateException("Chưa tải mô hình Whisper offline. Vui lòng mở Cài đặt để tải mô hình.")
            )
        }

        if (!audioFile.exists()) {
            return Result.failure(
                IllegalArgumentException("Tệp âm thanh không tồn tại: ${audioFile.absolutePath}")
            )
        }

        return withContext(Dispatchers.Default) {
            var tempWavFile: File? = null
            var whisperModel: WhisperModel? = null

            try {
                // 1. Chuẩn bị file WAV 16kHz Mono
                val wavFile = if (audioFile.extension.lowercase() == "wav") {
                    audioFile
                } else {
                    val temp = File(context.cacheDir, "whisper_temp_${System.currentTimeMillis()}.wav")
                    tempWavFile = temp
                    val convertResult = audioConverter(audioFile, temp)
                    if (convertResult.isFailure) {
                        val err = convertResult.exceptionOrNull() ?: Exception("Lỗi chuyển đổi file âm thanh")
                        return@withContext Result.failure(err)
                    }
                    temp
                }

                Log.i(TAG, "Bắt đầu nạp mô hình: ${modelFile.name}")
                // 2. Nạp model qua Whisper library
                whisperModel = Whisper.loadModel(context, modelFile.absolutePath)
                if (!whisperModel.isValid) {
                    return@withContext Result.failure(
                        IllegalStateException("Không thể nạp mô hình Whisper từ file: ${modelFile.name}")
                    )
                }

                // 3. Cấu hình nhận diện tiếng Việt
                val config = WhisperConfig(
                    language = "vi",
                    translate = false,
                    threads = 4,
                    maxSegmentLength = 0,
                    printTimestamps = false
                )

                Log.i(TAG, "Bắt đầu nhận diện âm thanh on-device: ${wavFile.name}")
                val whisperResult = Whisper.transcribe(whisperModel, wavFile.absolutePath, config)
                val text = whisperResult.text.trim()

                Log.i(TAG, "Nhận diện whisper.cpp offline thành công (${whisperResult.processingTimeMs} ms): $text")
                Result.success(text)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi nhận diện offline bằng whisper.cpp", e)
                Result.failure(e)
            } finally {
                whisperModel?.let {
                    try {
                        Whisper.releaseModel(it)
                    } catch (_: Exception) {}
                }
                tempWavFile?.let {
                    if (it.exists()) it.delete()
                }
            }
        }
    }
}
