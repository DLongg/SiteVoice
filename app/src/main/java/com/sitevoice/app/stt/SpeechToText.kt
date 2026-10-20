package com.sitevoice.app.stt

import java.io.File

interface SpeechToText {
    suspend fun transcribe(audioFile: File): Result<String>
}
