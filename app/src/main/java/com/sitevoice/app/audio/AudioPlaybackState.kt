package com.sitevoice.app.audio

sealed class AudioPlaybackState {
    object Idle : AudioPlaybackState()
    data class Playing(val filePath: String) : AudioPlaybackState()
    data class Paused(val filePath: String) : AudioPlaybackState()
    data class Error(val message: String) : AudioPlaybackState()
}
