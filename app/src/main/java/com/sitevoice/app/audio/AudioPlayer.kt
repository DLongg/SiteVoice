package com.sitevoice.app.audio

import android.media.MediaPlayer
import android.util.Log

class AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPath: String? = null

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    fun play(filePath: String, onCompletion: () -> Unit) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    currentPlayingPath = null
                    onCompletion()
                }
                start()
            }
            currentPlayingPath = filePath
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Lỗi phát file âm thanh: $filePath", e)
            stop()
            onCompletion()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        currentPlayingPath = null
    }
}
