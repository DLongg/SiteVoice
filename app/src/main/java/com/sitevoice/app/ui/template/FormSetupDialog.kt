package com.sitevoice.app.ui.template

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sitevoice.app.R
import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormLlmDescriber
import com.sitevoice.app.formsetup.FormSetupAnalysis
import com.sitevoice.app.settings.SecurePreferences
import java.util.Locale
import androidx.compose.material3.MaterialTheme
import com.sitevoice.app.ui.theme.SiteVoiceTheme
import com.sitevoice.app.ui.theme.TextSizes
import com.sitevoice.app.ui.theme.Spacing

private val LOAI_OPTIONS = listOf(
    "danh_sach" to "Danh sách",
    "van_ban" to "Văn bản",
    "so" to "Số lượng",
    "ngay" to "Ngày tháng",
    "o_tich" to "Ô tích"
)

private val NGUON_OPTIONS = listOf(
    "thi_cong" to "Thi công",
    "nghiem_thu" to "Nghiệm thu",
    "an_toan" to "An toàn & VSMT",
    "nhap_tay" to "Nhập tay cuối ngày",
    "vat_tu" to "Vật tư",
    "may_moc" to "Máy móc",
    "thoi_tiet" to "Thời tiết",
    "nhan_luc" to "Nhân lực",
    "y_kien_cdt" to "Ý kiến CĐT",
    "y_kien_tvgs" to "Ý kiến TVGS",
    "y_kien_nha_thau" to "Ý kiến Nhà thầu"
)

private fun formatLoaiLabel(loai: String): String {
    return LOAI_OPTIONS.firstOrNull { it.first.equals(loai, ignoreCase = true) }?.second ?: loai.ifBlank { "Tự động" }
}

private fun formatNguonLabel(nguon: String): String {
    return NGUON_OPTIONS.firstOrNull { it.first.equals(nguon, ignoreCase = true) }?.second ?: nguon.ifBlank { "Chung" }
}

enum class FormSetupStep {
    STEP_1_TAGS,
    STEP_2_PROMPT_CONFIG
}

@Composable
fun FormSetupDialog(
    analysis: FormSetupAnalysis?,
    isLoading: Boolean,
    errorMessage: String?,
    onConfirmSetup: (templateName: String, approvedCandidates: List<FormCandidate>, systemPrompt: String, temperature: Float) -> Unit,
    onRescan: () -> Unit = {},
    onDismiss: () -> Unit,
    initialTemperature: Float? = null
) {
    val context = LocalContext.current
    val securePrefs = remember(context) { SecurePreferences(context) }

    var currentStep by remember { mutableStateOf(FormSetupStep.STEP_1_TAGS) }
    var templateName by remember(analysis) {
        mutableStateOf(analysis?.originalDocTitle?.replace("[SiteVoice Mẫu] ", "") ?: "Mẫu tự động")
    }

    // Danh sách ứng viên có thể bật/tắt hoặc chỉnh sửa tay trực tiếp
    var candidateList by remember(analysis) {
        mutableStateOf(analysis?.candidates.orEmpty())
    }

    // ID của candidate đang được mở rộng chỉnh sửa (null nếu không có)
    var editingCandidateId by remember { mutableStateOf<Int?>(null) }

    // Prompt thống nhất do AI sinh từ các thẻ neo
    var promptText by remember { mutableStateOf("") }

    // Nhiệt độ biên tập (0.0f .. 0.8f)
    var temperature by remember {
        mutableStateOf(initialTemperature ?: securePrefs.llmTemperature)
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.xl)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.form_setup_dialog_title),
                            fontSize = TextSizes.title,
                            lineHeight = TextSizes.titleLine,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.form_setup_dialog_subtitle),
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isLoading) {
                        Text("✕", fontSize = TextSizes.title, lineHeight = TextSizes.titleLine, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Trạng thái đang phân tích
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            Text(
                                text = stringResource(R.string.form_setup_loading_title),
                                fontSize = TextSizes.body,
                                lineHeight = TextSizes.bodyLine,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.form_setup_loading_subtitle),
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (!errorMessage.isNullOrBlank()) {
                    // Trạng thái lỗi
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(Spacing.lg)) {
                                Text(
                                    text = stringResource(R.string.form_setup_error_title),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(text = errorMessage, fontSize = TextSizes.body, lineHeight = TextSizes.bodyLine, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                } else if (analysis != null) {
                    // Thanh Stepper Indicator 2 bước chuẩn App Screens
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentStep == FormSetupStep.STEP_1_TAGS) {
                                    stringResource(R.string.form_setup_step1_indicator)
                                } else {
                                    stringResource(R.string.form_setup_step2_indicator)
                                },
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (currentStep == FormSetupStep.STEP_1_TAGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(width = 24.dp, height = 4.dp)
                                ) {}
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (currentStep == FormSetupStep.STEP_2_PROMPT_CONFIG) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(width = 24.dp, height = 4.dp)
                                ) {}
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    if (currentStep == FormSetupStep.STEP_1_TAGS) {
                        // ==================== BƯỚC 1: DUYỆT THẺ NEO ====================
                        Text(
                            text = stringResource(R.string.form_setup_template_name_label),
                            fontSize = TextSizes.body,
                            lineHeight = TextSizes.bodyLine,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )

                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Khối Bản sao an toàn & Mở Google Docs gõ sửa
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(Spacing.md)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.form_setup_safe_copy_label),
                                            fontSize = TextSizes.caption,
                                            lineHeight = TextSizes.captionLine,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = analysis.originalDocTitle,
                                            fontSize = TextSizes.caption,
                                            lineHeight = TextSizes.captionLine,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.sm))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(analysis.workingDocUrl))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.weight(1.3f),
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.sm)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.form_setup_open_in_docs),
                                            fontSize = TextSizes.caption,
                                            lineHeight = TextSizes.captionLine,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = onRescan,
                                        enabled = !isLoading,
                                        modifier = Modifier.weight(0.7f),
                                        shape = MaterialTheme.shapes.small,
                                        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.sm)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.form_setup_rescan),
                                            fontSize = TextSizes.caption,
                                            lineHeight = TextSizes.captionLine,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = stringResource(R.string.form_setup_rescan_hint),
                                    fontSize = TextSizes.caption,
                                    lineHeight = TextSizes.captionLine,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        val activeCount = candidateList.count { !it.boQua }
                        val skippedCount = candidateList.size - activeCount

                        Text(
                            text = stringResource(R.string.form_setup_candidates_header, activeCount, skippedCount),
                            fontSize = TextSizes.body,
                            lineHeight = TextSizes.bodyLine,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = stringResource(R.string.form_setup_candidates_hint),
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.sm)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                                .padding(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(candidateList, key = { it.id }) { item ->
                                val isExpanded = (editingCandidateId == item.id)

                                CandidateCard(
                                    candidate = item,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        editingCandidateId = if (isExpanded) null else item.id
                                    },
                                    onUpdate = { updated ->
                                        candidateList = candidateList.map {
                                            if (it.id == updated.id) updated else it
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Navigation Bar Bước 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = {
                                    if (promptText.isBlank()) {
                                        promptText = FormLlmDescriber.generateUnifiedPrompt(candidateList, templateName)
                                    }
                                    currentStep = FormSetupStep.STEP_2_PROMPT_CONFIG
                                },
                                modifier = Modifier.weight(2f),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = stringResource(R.string.form_setup_step1_continue),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    } else {
                        // ==================== BƯỚC 2: DUYỆT PROMPT & NHIỆT ĐỘ ====================
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // 1. Khối thanh trượt Nhiệt độ biên tập (Temperature)
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.form_setup_temperature_title),
                                            fontSize = TextSizes.body,
                                            lineHeight = TextSizes.bodyLine,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%.2f", temperature),
                                                fontSize = TextSizes.caption,
                                                lineHeight = TextSizes.captionLine,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    Text(
                                        text = stringResource(R.string.form_setup_temperature_desc),
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    Slider(
                                        value = temperature,
                                        onValueChange = { newTemp ->
                                            temperature = (Math.round(newTemp * 100) / 100f).coerceIn(0.0f, 0.8f)
                                        },
                                        valueRange = 0.0f..0.8f,
                                        steps = 15,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = stringResource(R.string.form_setup_temperature_hint),
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Spacing.md))

                            // 2. Khối soạn thảo Prompt thống nhất
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.form_setup_prompt_label),
                                    fontSize = TextSizes.body,
                                    lineHeight = TextSizes.bodyLine,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedButton(
                                    onClick = {
                                        promptText = FormLlmDescriber.generateUnifiedPrompt(candidateList, templateName)
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs)
                                ) {
                                    Text(
                                        text = stringResource(R.string.form_setup_step2_regenerate),
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.form_setup_prompt_hint),
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                minLines = 8,
                                maxLines = 16,
                                shape = MaterialTheme.shapes.medium,
                                textStyle = TextStyle(
                                    fontSize = TextSizes.caption,
                                    lineHeight = TextSizes.captionLine,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Navigation Bar Bước 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = FormSetupStep.STEP_1_TAGS },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = stringResource(R.string.form_setup_step2_back),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    onConfirmSetup(templateName, candidateList, promptText, temperature)
                                },
                                modifier = Modifier.weight(2f),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = stringResource(R.string.form_setup_step2_complete),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormSetupDialog(
    analysis: FormSetupAnalysis?,
    isLoading: Boolean,
    errorMessage: String?,
    onConfirmSetup: (templateName: String, approvedCandidates: List<FormCandidate>) -> Unit,
    onRescan: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val securePrefs = remember(context) { SecurePreferences(context) }
    FormSetupDialog(
        analysis = analysis,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onConfirmSetup = { templateName, candidates, prompt, temp ->
            securePrefs.llmTemperature = temp
            securePrefs.setString("pending_setup_prompt", prompt)
            onConfirmSetup(templateName, candidates)
        },
        onRescan = onRescan,
        onDismiss = onDismiss,
        initialTemperature = securePrefs.llmTemperature
    )
}

@Composable
private fun CandidateCard(
    candidate: FormCandidate,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (FormCandidate) -> Unit
) {
    val isIncluded = !candidate.boQua

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isExpanded) {
            MaterialTheme.colorScheme.background
        } else if (isIncluded) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = androidx.compose.foundation.BorderStroke(
            if (isExpanded) 1.5.dp else 1.dp,
            if (isExpanded) MaterialTheme.colorScheme.primary else if (isIncluded) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            if (!isExpanded) {
                // CHẾ ĐỘ THU GỌN (COLLAPSED)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isIncluded,
                        onCheckedChange = { checked ->
                            onUpdate(candidate.copy(boQua = !checked))
                        }
                    )

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "{{${candidate.ma}}}",
                                fontWeight = FontWeight.Bold,
                                fontSize = TextSizes.body,
                                lineHeight = TextSizes.bodyLine,
                                color = if (isIncluded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Badge Loại
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = formatLoaiLabel(candidate.loai),
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                                    )
                                }

                                // Badge Nguồn
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = formatNguonLabel(candidate.nguon),
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        Text(
                            text = candidate.ten.ifBlank { "(Chưa đặt tên nhãn)" },
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.Medium,
                            color = if (isIncluded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = MaterialTheme.shapes.small,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
                                if (candidate.tieuDeGanNhat.isNotBlank()) {
                                    Text(
                                        text = "📍 Đề mục: ${candidate.tieuDeGanNhat}",
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "Neo vị trí (#${candidate.startIndex}..#${candidate.endIndex}): Trước \"${candidate.truoc}\" | Sau \"${candidate.sau}\"",
                                    fontSize = TextSizes.caption,
                                    lineHeight = TextSizes.captionLine,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    // Nút bấm sửa
                    TextButton(
                        onClick = onToggleExpand,
                        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs)
                    ) {
                        Text(
                            text = "Sửa ✏️",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // CHẾ ĐỘ CHỈNH SỬA TAY TRỰC TIẾP (EXPANDED EDIT FORM)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✏️ Chỉnh sửa chỗ điền #${candidate.id}",
                        fontSize = TextSizes.body,
                        lineHeight = TextSizes.bodyLine,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", fontSize = TextSizes.body, lineHeight = TextSizes.bodyLine, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // 1. Mã placeholder
                Text(
                    text = "Mã thẻ placeholder (ghi vào Doc dạng {{...}}):",
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                OutlinedTextField(
                    value = candidate.ma,
                    onValueChange = { input ->
                        val normalized = input.uppercase().replace(" ", "_")
                        onUpdate(candidate.copy(ma = normalized))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    trailingIcon = {
                        Text(
                            text = "{{${candidate.ma}}}",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = Spacing.sm)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // 2. Tên mục
                Text(
                    text = "Tên mục hiển thị trong báo cáo:",
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                OutlinedTextField(
                    value = candidate.ten,
                    onValueChange = { onUpdate(candidate.copy(ten = it)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // 3. Kiểu dữ liệu (loai)
                Text(
                    text = "Kiểu dữ liệu:",
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    LOAI_OPTIONS.forEach { (key, label) ->
                        val isSelected = candidate.loai.equals(key, ignoreCase = true)
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.clickable {
                                onUpdate(candidate.copy(loai = key))
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // 4. Nguồn dữ liệu (nguon)
                Text(
                    text = "Nguồn dữ liệu bóc tách:",
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    NGUON_OPTIONS.forEach { (key, label) ->
                        val isSelected = candidate.nguon.equals(key, ignoreCase = true)
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.clickable {
                                onUpdate(candidate.copy(nguon = key))
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // 5. Vị trí neo trong bản mẫu Google Doc (Xem & Sửa trực tiếp bằng tay)
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 Vị trí neo trong bản mẫu Google Doc:",
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tín hiệu: ${candidate.tinHieu.ifBlank { "dong_cham" }}",
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        OutlinedTextField(
                            value = candidate.tieuDeGanNhat,
                            onValueChange = { onUpdate(candidate.copy(tieuDeGanNhat = it)) },
                            label = { Text("Đề mục chứa chỗ điền (tieuDeGanNhat)", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            OutlinedTextField(
                                value = candidate.truoc,
                                onValueChange = { onUpdate(candidate.copy(truoc = it)) },
                                label = { Text("Ngữ cảnh trước (truoc)", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small
                            )
                            OutlinedTextField(
                                value = candidate.sau,
                                onValueChange = { onUpdate(candidate.copy(sau = it)) },
                                label = { Text("Ngữ cảnh sau (sau)", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            OutlinedTextField(
                                value = candidate.startIndex.toString(),
                                onValueChange = { str ->
                                    val idx = str.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: candidate.startIndex
                                    onUpdate(candidate.copy(startIndex = idx))
                                },
                                label = { Text("Ký tự bắt đầu (startIndex)", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small
                            )
                            OutlinedTextField(
                                value = candidate.endIndex.toString(),
                                onValueChange = { str ->
                                    val idx = str.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: candidate.endIndex
                                    onUpdate(candidate.copy(endIndex = idx))
                                },
                                label = { Text("Ký tự kết thúc (endIndex)", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // 6. Thao tác phụ: Bỏ qua / Áp dụng & Nút Xong
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onUpdate(candidate.copy(boQua = !candidate.boQua))
                        },
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (candidate.boQua) SiteVoiceTheme.colors.success else MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(
                            text = if (candidate.boQua) "✓ Khôi phục vị trí này" else "✕ Bỏ qua vị trí này",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onToggleExpand,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "✓ Xong",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
