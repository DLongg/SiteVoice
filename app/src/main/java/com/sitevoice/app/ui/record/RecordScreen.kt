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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sitevoice.app.R
import com.sitevoice.app.audio.AudioPlayer
import com.sitevoice.app.audio.AudioRecorder
import kotlinx.coroutines.delay
import java.io.File

data class LastRecordedItem(
    val file: File,
    val durationSeconds: Long
)

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val recorder = remember { AudioRecorder(context) }
    val player = remember { AudioPlayer() }

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
        color = Color(0xFFF6F7F9) // Nền phẳng trung tính sạch sẽ theo chuẩn design-taste-frontend
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header tiêu chuẩn
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.welcome_subtitle),
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }

            // Phần trung tâm: Nút ghi âm một chạm
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
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
                            fontSize = 36.sp,
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

                    Spacer(modifier = Modifier.height(32.dp))

                    // Nút nhấn giữ ghi âm trung tâm (Đường kính 100dp)
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
                        // Icon Micro đơn giản bằng Shape/Text nếu không dùng icon ngoài
                        Text(
                            text = if (isRecording) "■" else "●",
                            color = Color.White,
                            fontSize = if (isRecording) 28.sp else 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Phần chân trang: Thẻ bản ghi gần nhất (nếu có)
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
                    }
                }
            }
        }
    }
}
