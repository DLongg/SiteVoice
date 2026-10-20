package com.sitevoice.app.ui.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sitevoice.app.R
import com.sitevoice.app.audio.AudioPlayer
import com.sitevoice.app.audio.AudioRecorder
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.stt.GroqWhisper
import com.sitevoice.app.ui.settings.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

data class LastRecordedItem(
    val file: File,
    val durationSeconds: Long
)

sealed class SttState {
    object Idle : SttState()
    object Transcribing : SttState()
    data class Success(val text: String) : SttState()
    data class Error(val message: String) : SttState()
    object NoApiKey : SttState()
}

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val securePrefs = remember { SecurePreferences(context) }
    val recorder = remember { AudioRecorder(context) }
    val player = remember { AudioPlayer() }
    val groqWhisper = remember { GroqWhisper(apiKeyProvider = { securePrefs.groqApiKey }) }

    var showSettingsDialog by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordingTimerSeconds by remember { mutableLongStateOf(0L) }
    var lastRecordedItem by remember { mutableStateOf<LastRecordedItem?>(null) }
    var isPlayingLastRecord by remember { mutableStateOf(false) }
    var sttState by remember { mutableStateOf<SttState>(SttState.Idle) }

    // Hàm gọi STT
    fun runStt(audioFile: File) {
        val apiKey = securePrefs.groqApiKey
        if (apiKey.isNullOrBlank()) {
            sttState = SttState.NoApiKey
            return
        }

        sttState = SttState.Transcribing
        scope.launch {
            val result = groqWhisper.transcribe(audioFile)
            sttState = if (result.isSuccess) {
                SttState.Success(result.getOrThrow())
            } else {
                SttState.Error(result.exceptionOrNull()?.message ?: "Lỗi chuyển đổi STT")
            }
        }
    }

    // Timer đếm giây khi đang ghi âm
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTimerSeconds = 0L
            while (isRecording) {
                delay(1000L)
                recordingTimerSeconds++
            }
        }
    }

    // Cleanup khi thoát màn hình
    DisposableEffect(Unit) {
        onDispose {
            recorder.cancelRecording()
            player.stop()
        }
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "ButtonScaleAnimation"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF6F7F9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header tiêu chuẩn kèm nút Cài đặt API
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = R.string.welcome_subtitle),
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                OutlinedButton(
                    onClick = { showSettingsDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (securePrefs.groqApiKey.isNullOrBlank()) "Cài API Key" else "API Key ✓",
                        fontSize = 12.sp,
                        color = if (securePrefs.groqApiKey.isNullOrBlank()) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Phần trung tâm: Nút ghi âm một chạm
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!hasPermission) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.record_permission_required),
                                fontSize = 14.sp,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = stringResource(id = R.string.record_btn_grant_permission))
                            }
                        }
                    }
                } else {
                    // Hiển thị bộ đếm thời gian
                    if (isRecording) {
                        val minutes = recordingTimerSeconds / 60
                        val seconds = recordingTimerSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.record_status_recording),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626)
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.record_hint_ready),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4B5563)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Nút nhấn giữ ghi âm trung tâm
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(108.dp)
                            .scale(buttonScale)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) Color(0xFFDC2626) else Color(0xFF1E293B)
                            )
                            .border(
                                width = if (isRecording) 4.dp else 2.dp,
                                color = if (isRecording) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                                shape = CircleShape
                            )
                            .pointerInput(hasPermission) {
                                detectTapGestures(
                                    onPress = {
                                        if (!hasPermission) {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@detectTapGestures
                                        }

                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val startResult = recorder.startRecording()
                                        if (startResult.isSuccess) {
                                            isRecording = true
                                        }

                                        val released = tryAwaitRelease()

                                        if (released) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val stopResult = recorder.stopRecording()
                                            isRecording = false
                                            if (stopResult.isSuccess && startResult.isSuccess) {
                                                val duration = stopResult.getOrNull() ?: 0L
                                                val file = startResult.getOrNull()
                                                if (file != null && duration >= 800L) {
                                                    lastRecordedItem = LastRecordedItem(
                                                        file = file,
                                                        durationSeconds = duration / 1000L
                                                    )
                                                    // Tự động gọi STT sau khi ghi âm thành công
                                                    runStt(file)
                                                }
                                            }
                                        } else {
                                            recorder.cancelRecording()
                                            isRecording = false
                                        }
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = if (isRecording) "■" else "●",
                            color = Color.White,
                            fontSize = if (isRecording) 28.sp else 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Phần kết quả: Thẻ bản ghi âm và kết quả STT
            lastRecordedItem?.let { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.record_recent_card_title),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF111827)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(id = R.string.record_duration, item.durationSeconds),
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isPlayingLastRecord) {
                                        player.stop()
                                        isPlayingLastRecord = false
                                    } else {
                                        player.play(item.file.absolutePath) {
                                            isPlayingLastRecord = false
                                        }
                                        isPlayingLastRecord = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isPlayingLastRecord)
                                        stringResource(id = R.string.record_stop)
                                    else
                                        stringResource(id = R.string.record_play)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Khối hiển thị trạng thái STT
                        when (val state = sttState) {
                            is SttState.Idle -> {
                                OutlinedButton(
                                    onClick = { runStt(item.file) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Chuyển thành văn bản (STT)")
                                }
                            }
                            is SttState.Transcribing -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF2563EB)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Đang chuyển văn bản qua Groq Whisper...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                            is SttState.Success -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Văn bản nhận diện (STT):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.text,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A),
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                            is SttState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Lỗi STT: ${state.message}",
                                        fontSize = 13.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { runStt(item.file) },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Thử lại STT", fontSize = 12.sp)
                                    }
                                }
                            }
                            is SttState.NoApiKey -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chưa có Groq API Key",
                                        fontSize = 13.sp,
                                        color = Color(0xFFB45309)
                                    )
                                    Button(
                                        onClick = { showSettingsDialog = true },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                    ) {
                                        Text("Cài đặt key", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hộp thoại cài đặt API key
        if (showSettingsDialog) {
            SettingsDialog(
                currentApiKey = securePrefs.groqApiKey,
                onSaveKey = { newKey ->
                    securePrefs.groqApiKey = newKey
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}
