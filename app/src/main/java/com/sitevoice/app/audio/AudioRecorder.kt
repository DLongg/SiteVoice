package com.sitevoice.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMs: Long = 0

    val isRecording: Boolean
        get() = mediaRecorder != null

    /**
     * Bắt đầu ghi âm và lưu ra file M4A.
     * Quy tắc cốt lõi: Luôn tạo và mở file trước khi bắt đầu ghi.
     */
    fun startRecording(): Result<File> {
        return try {
            val audioDir = File(context.filesDir, "audio").apply {
                if (!exists()) mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(audioDir, "audio_$timeStamp.m4a")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentOutputFile = outputFile
            startTimeMs = System.currentTimeMillis()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Lỗi khi khởi chạy MediaRecorder", e)
            releaseRecorder()
            Result.failure(e)
        }
    }

    /**
     * Dừng ghi và trả về thời lượng ghi âm (milliseconds).
     * Khi dừng thành công, file âm thanh đã trở thành bản ghi chính thức,
     * gán currentOutputFile = null để tránh bị xóa nhầm khi cancelRecording.
     */
    fun stopRecording(): Result<Long> {
        val recorder = mediaRecorder ?: return Result.failure(IllegalStateException("Recorder chưa được bật"))
        return try {
            val durationMs = System.currentTimeMillis() - startTimeMs
            recorder.stop()
            releaseRecorder()
            currentOutputFile = null // File ghi thành công, không cho phép xóa
            Result.success(durationMs)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Lỗi khi dừng MediaRecorder", e)
            releaseRecorder()
            Result.failure(e)
        }
    }

    /**
     * Hủy bản ghi âm đang thực hiện dở (ví dụ người dùng trượt tay hủy).
     * Chỉ xóa file nếu đang thực sự trong quá trình ghi (mediaRecorder != null).
     */
    fun cancelRecording() {
        val wasRecording = mediaRecorder != null
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}
        releaseRecorder()

        if (wasRecording) {
            currentOutputFile?.let {
                if (it.exists()) {
                    it.delete()
                    Log.i("AudioRecorder", "Đã xóa file ghi âm bị hủy: ${it.name}")
                }
            }
        }
        currentOutputFile = null
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
    }
}
