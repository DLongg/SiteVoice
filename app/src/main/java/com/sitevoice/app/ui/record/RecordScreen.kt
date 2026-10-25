package com.sitevoice.app.ui.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sitevoice.app.R
import com.sitevoice.app.audio.AudioRecorder
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.ui.CurrentSttStatus
import com.sitevoice.app.ui.MainViewModel
import kotlinx.coroutines.delay
import java.io.File

data class LastRecordedItem(
    val file: File,
    val durationSeconds: Long
)

@Composable
fun RecordScreen(
    viewModel: MainViewModel,
    securePreferences: SecurePreferences,
    onOpenSettings: () -> Unit,
    onOpenTemplateLibrary: () -> Unit = {},
    activeTemplateName: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val recorder = remember { AudioRecorder(context) }
    val audioPlayer = viewModel.audioPlayer
    val sttStatus by viewModel.currentSttStatus.collectAsState()
    val activeNotes by viewModel.activeNotes.collectAsState()

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

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTimerSeconds = 0L
            while (isRecording) {
                delay(1000L)
                recordingTimerSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.cancelRecording()
            audioPlayer.stop()
        }
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 120),
        label = "ButtonScaleAnimation"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF3EFE6)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Architectural Ledger Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14171A))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SỔ NHẬT KÝ CÔNG TRƯỜNG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFFD84315)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = Color(0xFFFAF8F3)
                        )
                    }

                    val hasStt = !securePreferences.groqApiKey.isNullOrBlank()
                    val hasClassify = !securePreferences.geminiApiKey.isNullOrBlank()
                    val (statusText, statusBg, statusFg) = when {
                        hasStt && hasClassify -> Triple("API SẴN SÀNG", Color(0xFF1B4332), Color(0xFF74C69D))
                        hasStt -> Triple("STT + GROQ", Color(0xFF1B4332), Color(0xFF74C69D))
                        hasClassify -> Triple("THIẾU STT KEY", Color(0xFF5F370E), Color(0xFFFBD38D))
                        else -> Triple("CÀI ĐẶT API", Color(0xFFD84315), Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(4.dp))
                            .clickable { onOpenSettings() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = statusFg
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ledger Meta Strip: Date + Active Template + Today's Note Count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF21262B), RoundedCornerShape(4.dp))
                        .clickable { onOpenTemplateLibrary() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BIỂU MẪU ĐANG ÁP DỤNG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF949A9E)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeTemplateName ?: "Nhật ký thi công công trường",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFAF8F3)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF14171A), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${activeNotes.size} GHI CHÚ",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFAF8F3)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ĐỔI MẪU →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD84315)
                        )
                    }
                }
            }

            // Main Field-Recording Deck
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasPermission) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8F3)),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD84315)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "YÊU CẦU QUYỀN MICROPHONE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(id = R.string.record_permission_required),
                                fontSize = 14.sp,
                                color = Color(0xFF14171A)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.record_btn_grant_permission),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Industrial Push-To-Talk Console Panel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAF8F3), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFFD6CFC2), RoundedCornerShape(4.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRecording) "ĐANG THU ÂM HIỆN TRƯỜNG" else "BỘ ĐÀM GHI NHẬT KÝ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (isRecording) Color(0xFFD84315) else Color(0xFF5C6268)
                            )
                            Text(
                                text = viewModel.currentDate,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF5C6268)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val minutes = recordingTimerSeconds / 60
                        val seconds = recordingTimerSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 52.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = if (isRecording) Color(0xFFD84315) else Color(0xFF14171A)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isRecording)
                                stringResource(id = R.string.record_status_recording)
                            else
                                stringResource(id = R.string.record_hint_ready),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isRecording) Color(0xFFD84315) else Color(0xFF5C6268)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Tactile Architectural Push-To-Talk Trigger
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .scale(buttonScale)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isRecording) Color(0xFFD84315) else Color(0xFF14171A)
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isRecording) Color(0xFFFAF8F3) else Color(0xFF3E444A),
                                    shape = RoundedCornerShape(4.dp)
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
                                                        viewModel.onRecordFinished(file, duration)
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isRecording) "[ NHẢ TAY ĐỂ LƯU GHI CHÚ ]" else "NHẤN & GIỮ ĐỂ NÓI",
                                    color = Color(0xFFFAF8F3),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isRecording)
                                        "Âm thanh gốc đang được ghi trực tiếp vào bộ nhớ máy"
                                    else
                                        "Tự động nhận diện giọng nói & hiệu đính thuật ngữ xây dựng",
                                    color = Color(0xFFD6CFC2),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Latest Recorded Item & STT Dispatch Card
                lastRecordedItem?.let { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8F3)),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6CFC2))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.record_recent_card_title).uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = Color(0xFF5C6268)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.record_duration, item.durationSeconds),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14171A)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (isPlayingLastRecord) {
                                            audioPlayer.stop()
                                            isPlayingLastRecord = false
                                        } else {
                                            audioPlayer.play(item.file.absolutePath) {
                                                isPlayingLastRecord = false
                                            }
                                            isPlayingLastRecord = true
                                        }
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isPlayingLastRecord)
                                            stringResource(id = R.string.record_stop)
                                        else
                                            stringResource(id = R.string.record_play),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14171A)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            when (val state = sttStatus) {
                                is CurrentSttStatus.Idle -> {
                                    Text(
                                        text = "Đã lưu vào sổ ghi chú hôm nay",
                                        fontSize = 13.sp,
                                        color = Color(0xFF5C6268)
                                    )
                                }
                                is CurrentSttStatus.Transcribing -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFEBE5D8), RoundedCornerShape(4.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFFD84315)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Đang chuyển giọng nói sang văn bản & hiệu đính ngữ cảnh...",
                                            fontSize = 13.sp,
                                            color = Color(0xFF14171A)
                                        )
                                    }
                                }
                                is CurrentSttStatus.Success -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFEBE5D8), RoundedCornerShape(4.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "NỘI DUNG ĐÃ NHẬN DIỆN & HIỆU ĐÍNH:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B4332)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.text,
                                            fontSize = 15.sp,
                                            color = Color(0xFF14171A),
                                            lineHeight = 21.sp
                                        )
                                    }
                                }
                                is CurrentSttStatus.Error -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFDE8E8), RoundedCornerShape(4.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "Lỗi STT: ${state.message}",
                                            fontSize = 13.sp,
                                            color = Color(0xFF9B1C1C)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.onRecordFinished(item.file, item.durationSeconds * 1000)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Thử lại STT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                is CurrentSttStatus.NoApiKey -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Chưa cài đặt Groq API Key",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                        Button(
                                            onClick = onOpenSettings,
                                            shape = RoundedCornerShape(4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315))
                                        ) {
                                            Text("Cài đặt ngay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
