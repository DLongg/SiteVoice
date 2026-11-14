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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sitevoice.app.R
import com.sitevoice.app.audio.AudioRecorder
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.ui.CurrentSttStatus
import com.sitevoice.app.ui.MainViewModel
import com.sitevoice.app.ui.theme.SiteVoiceTheme
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

    val colors = MaterialTheme.colorScheme
    val extraColors = SiteVoiceTheme.colors
    val type = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    val spacing = SiteVoiceTheme.spacing

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
            audioPlayer.stop()
        }
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "ButtonScaleAnimation"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.xl)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header tiêu chuẩn kèm nút Cài đặt API
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = type.titleLarge,
                        color = colors.onBackground
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = stringResource(id = R.string.welcome_subtitle),
                        style = type.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }

                val hasStt = !securePreferences.groqApiKey.isNullOrBlank()
                val hasClassify = !securePreferences.geminiApiKey.isNullOrBlank()
                val (statusText, statusColor) = when {
                    hasStt && hasClassify -> "STT ✓ | Gemini ✓" to extraColors.success
                    hasStt -> "STT ✓ | Phân loại: Groq" to extraColors.success
                    hasClassify -> "Thiếu STT Key" to extraColors.warning
                    else -> "Cài API Key" to colors.error
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = shapes.medium
                ) {
                    Text(
                        text = statusText,
                        style = type.labelMedium,
                        color = statusColor
                    )
                }
            }

            // Thanh hiển thị và đổi nhanh Mẫu báo cáo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.md),
                horizontalArrangement = Arrangement.Start
            ) {
                OutlinedButton(
                    onClick = onOpenTemplateLibrary,
                    shape = shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface)
                ) {
                    Text(
                        text = if (activeTemplateName != null) "📑 Mẫu: $activeTemplateName (Sửa)" else "✨ Thiết lập Nhật ký tự động ▸",
                        style = type.labelLarge,
                        color = colors.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            // Phần trung tâm: Nút ghi âm một chạm
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!hasPermission) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = extraColors.warningContainer),
                        shape = shapes.large,
                        modifier = Modifier.padding(spacing.lg)
                    ) {
                        Column(
                            modifier = Modifier.padding(spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.record_permission_required),
                                style = type.bodyMedium,
                                color = extraColors.onWarningContainer
                            )
                            Spacer(modifier = Modifier.height(spacing.md))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = extraColors.warning,
                                    contentColor = extraColors.onWarning
                                ),
                                shape = shapes.medium
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
                            style = type.displaySmall.copy(fontFeatureSettings = "tnum"),
                            color = colors.error
                        )
                        Spacer(modifier = Modifier.height(spacing.sm))
                        Text(
                            text = stringResource(id = R.string.record_status_recording),
                            style = type.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = colors.error
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.record_hint_ready),
                            style = type.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.xl))

                    // Nút nhấn giữ ghi âm trung tâm
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(108.dp)
                            .scale(buttonScale)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) colors.error else colors.primary
                            )
                            .border(
                                width = if (isRecording) 4.dp else 0.dp,
                                color = if (isRecording) colors.errorContainer else Color.Transparent,
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
                                                    // Lưu ngay vào Room và kích hoạt STT qua ViewModel
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
                        Text(
                            text = if (isRecording) "■" else "●",
                            color = if (isRecording) colors.onError else colors.onPrimary,
                            style = type.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.xxl))

            // Phần kết quả: Thẻ bản ghi âm và kết quả STT
            lastRecordedItem?.let { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.lg),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = shapes.large
                ) {
                    Column(modifier = Modifier.padding(spacing.lg)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.record_recent_card_title),
                                    style = type.titleMedium,
                                    color = colors.onSurface
                                )
                                Spacer(modifier = Modifier.height(spacing.xs))
                                Text(
                                    text = stringResource(id = R.string.record_duration, item.durationSeconds),
                                    style = type.bodyMedium,
                                    color = colors.onSurfaceVariant
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
                                shape = shapes.medium
                            ) {
                                Text(
                                    text = if (isPlayingLastRecord)
                                        stringResource(id = R.string.record_stop)
                                    else
                                        stringResource(id = R.string.record_play)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Khối hiển thị trạng thái STT
                        when (val state = sttStatus) {
                            is CurrentSttStatus.Idle -> {
                                Text(
                                    text = "Đã lưu vào danh sách ghi chú",
                                    style = type.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            is CurrentSttStatus.Transcribing -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.surfaceVariant, shapes.medium)
                                        .padding(spacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.primary
                                    )
                                    Spacer(modifier = Modifier.width(spacing.md))
                                    Text(
                                        text = "Đang chuyển văn bản qua Groq Whisper...",
                                        style = type.bodyMedium,
                                        color = colors.onSurface
                                    )
                                }
                            }
                            is CurrentSttStatus.Success -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.surfaceVariant, shapes.medium)
                                        .padding(spacing.md)
                                ) {
                                    Text(
                                        text = "Văn bản nhận diện (STT):",
                                        style = type.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = extraColors.success
                                    )
                                    Spacer(modifier = Modifier.height(spacing.xs))
                                    Text(
                                        text = state.text,
                                        style = type.bodyMedium,
                                        color = colors.onSurface
                                    )
                                }
                            }
                            is CurrentSttStatus.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.errorContainer, shapes.medium)
                                        .padding(spacing.md)
                                ) {
                                    Text(
                                        text = "Lỗi STT: ${state.message}",
                                        style = type.bodyMedium,
                                        color = colors.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(spacing.sm))
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.onRecordFinished(item.file, item.durationSeconds * 1000)
                                        },
                                        shape = shapes.small
                                    ) {
                                        Text("Thử lại STT")
                                    }
                                }
                            }
                            is CurrentSttStatus.NoApiKey -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(extraColors.warningContainer, shapes.medium)
                                        .padding(spacing.md),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chưa có Groq API Key",
                                        style = type.bodyMedium,
                                        color = extraColors.onWarningContainer
                                    )
                                    Button(
                                        onClick = onOpenSettings,
                                        shape = shapes.small,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = extraColors.warning,
                                            contentColor = extraColors.onWarning
                                        )
                                    ) {
                                        Text("Cài đặt key")
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
