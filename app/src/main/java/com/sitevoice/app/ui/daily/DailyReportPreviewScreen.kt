package com.sitevoice.app.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.DailyReportEntity
import com.sitevoice.app.template.DailyReportContent
import org.json.JSONObject
import androidx.compose.material3.MaterialTheme
import com.sitevoice.app.ui.theme.SiteVoiceTheme
import com.sitevoice.app.ui.theme.TextSizes
import com.sitevoice.app.ui.theme.Spacing

@Composable
fun DailyReportPreviewScreen(
    currentDate: String,
    reportEntity: DailyReportEntity?,
    dailyInput: DailyInputEntity?,
    isSynthesizing: Boolean,
    isExporting: Boolean = false,
    activeTemplateName: String? = null,
    isDeveloperMode: Boolean = false,
    onUpdateRawJson: ((String) -> Unit)? = null,
    onApproveClick: () -> Unit,
    onResynthesizeClick: () -> Unit,
    onBackToEditClick: () -> Unit,
    onExportDocClick: () -> Unit,
    onOpenDocClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isSynthesizing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    Text(
                        text = stringResource(id = R.string.report_synthesizing_loading),
                        fontSize = TextSizes.subtitle,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = stringResource(id = R.string.report_synthesizing_budget_note),
                        fontSize = TextSizes.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            return@Surface
        }

        if (reportEntity == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Chưa có bản tổng hợp nào cho ngày $currentDate",
                        fontSize = TextSizes.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Button(
                        onClick = onResynthesizeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = stringResource(id = R.string.btn_generate_report))
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedButton(
                        onClick = onBackToEditClick,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = stringResource(id = R.string.report_btn_back_to_edit))
                    }
                }
            }
            return@Surface
        }

        val content = DailyReportContent.fromJson(reportEntity.contentJson)
        val isApproved = reportEntity.status == "APPROVED" || reportEntity.status == "EXPORTED"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))

            // Thanh điều hướng trên cùng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackToEditClick,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(text = "← " + stringResource(id = R.string.report_btn_back_to_edit), fontSize = TextSizes.caption)
                }

                // Status Chip
                StatusBadge(status = reportEntity.status)
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Tiêu đề
            Text(
                text = stringResource(id = R.string.report_preview_title),
                fontSize = TextSizes.title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val templateDisplayName = activeTemplateName ?: (if (reportEntity.templateId == "nhat-ky-ca-nhan") "Nhật ký cá nhân" else "Nhật ký thi công công trường")
            Text(
                text = "Ngày: $currentDate • Mẫu: $templateDisplayName",
                fontSize = TextSizes.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Banner phê duyệt nếu đã duyệt
            if (isApproved) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SiteVoiceTheme.colors.successContainer, MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                        .padding(Spacing.md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✓", fontSize = TextSizes.subtitle, fontWeight = FontWeight.Bold, color = SiteVoiceTheme.colors.success)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = stringResource(id = R.string.report_approved_banner),
                            fontSize = TextSizes.body,
                            fontWeight = FontWeight.Medium,
                            color = SiteVoiceTheme.colors.onSuccessContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            val isConstruction = reportEntity.templateId == "nhat-ky-cong-truong" || reportEntity.templateId.isBlank()

            if (isConstruction) {
                // Khối 1: Thông tin hiện trường (Thời tiết, Nhân lực, Máy móc)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = stringResource(id = R.string.report_section_general),
                            fontSize = TextSizes.body,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))

                        val weatherText = buildString {
                            val tq = content.allFields["tong_quan_thoi_tiet"]?.toString()?.trim()
                            val isMorningRain = content.allFields["s_m"] == true || content.allFields["s_m"] == "true" || dailyInput?.weatherMorning == "mua"
                            val isAfternoonRain = content.allFields["c_m"] == true || content.allFields["c_m"] == "true" || dailyInput?.weatherAfternoon == "mua"
                            val isEveningRain = content.allFields["t_m"] == true || content.allFields["t_m"] == "true" || dailyInput?.weatherEvening == "mua"

                            val morning = if (isMorningRain) "🌧️ Mưa" else "☀️ Nắng"
                            val afternoon = if (isAfternoonRain) "🌧️ Mưa" else "☀️ Nắng"
                            val evening = if (isEveningRain) "🌧️ Mưa" else "🌙 Bình thường"

                            append("Sáng: $morning  •  Chiều: $afternoon  •  Tối: $evening")
                            if (!tq.isNullOrBlank() && tq != "...") {
                                append(" ($tq)")
                            }
                        }
                        Text(text = "Thời tiết: $weatherText", fontSize = TextSizes.body, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(Spacing.xs))
                        val bch = content.allFields["ng_ch"]?.toString()?.toIntOrNull() ?: dailyInput?.commandStaffCount ?: 0
                        val cn = content.allFields["ng_cn"]?.toString()?.toIntOrNull() ?: dailyInput?.workerCount ?: 0
                        Text(text = "Lực lượng: Ban chỉ huy: $bch người  |  Công nhân: $cn người", fontSize = TextSizes.body, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(Spacing.xs))
                        val mayMocFromLlm = content.allFields["may_moc"]?.let {
                            if (it is List<*>) it.filterNotNull().joinToString(", ") else it.toString()
                        }
                        val eq = if (!mayMocFromLlm.isNullOrBlank()) mayMocFromLlm else (dailyInput?.equipmentList?.ifBlank { "Không ghi nhận máy móc chính" } ?: "Không ghi nhận máy móc chính")
                        Text(text = "Thiết bị: $eq", fontSize = TextSizes.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Khối 2: Công việc thi công (Ngân sách: ≤ 15 dòng)
                ReportSectionList(
                    title = stringResource(id = R.string.report_section_construction),
                    badgeText = "{{CONG_VIEC_THI_CONG}} • ${content.congViecThiCong.size}/15 dòng",
                    items = content.congViecThiCong
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Khối 3: Nghiệm thu công việc (Ngân sách: ≤ 6 dòng)
                ReportSectionList(
                    title = stringResource(id = R.string.report_section_inspection),
                    badgeText = "{{NGHIEM_THU}} • ${content.nghiemThu.size}/6 dòng",
                    items = content.nghiemThu
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Khối 4: An toàn lao động & VSMT (Ngân sách: ≤ 3 dòng)
                ReportSectionList(
                    title = stringResource(id = R.string.report_section_safety),
                    badgeText = "{{AN_TOAN}} • ${content.anToan.size}/3 dòng",
                    items = content.anToan
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Khối 5: Ý kiến các bên (Tối đa 200 ký tự mỗi bên)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = stringResource(id = R.string.report_section_opinions),
                            fontSize = TextSizes.body,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))

                        OpinionRow(label = stringResource(id = R.string.report_opinion_cdt) + " ({{Y_KIEN_CDT}})", content = content.yKienCdt)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        OpinionRow(label = stringResource(id = R.string.report_opinion_tvgs) + " ({{Y_KIEN_TVGS}})", content = content.yKienTvgs)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        OpinionRow(label = stringResource(id = R.string.report_opinion_contractor) + " ({{Y_KIEN_NHA_THAU}})", content = content.yKienNhaThau)
                    }
                }
            } else {
                // Hiển thị động theo các trường của Mẫu cá nhân / Mẫu tự tạo
                val dynamicFields = content.allFields.filterKeys { it != "rawJson" && it != "chua_phan_loai" }
                dynamicFields.forEach { (key, value) ->
                    val readableTitle = key.replace("_", " ").replaceFirstChar { it.uppercase() }
                    val placeholderTag = "{{${key.uppercase()}}}"
                    if (value is List<*>) {
                        val stringList = value.mapNotNull { it?.toString() }
                        ReportSectionList(
                            title = readableTitle,
                            badgeText = "$placeholderTag • ${stringList.size} mục",
                            items = stringList
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium,
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(Spacing.lg)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = readableTitle,
                                        fontSize = TextSizes.body,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = placeholderTag,
                                        fontSize = TextSizes.caption,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    text = value.toString().ifBlank { stringResource(id = R.string.report_empty_field) },
                                    fontSize = TextSizes.body,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                }
            }

            // Khối 6: Ghi chú chưa phân loại (nếu có)
            if (content.chuaPhanLoai.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SiteVoiceTheme.colors.warningContainer),
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ " + stringResource(id = R.string.report_section_unclassified),
                                fontSize = TextSizes.body,
                                fontWeight = FontWeight.Bold,
                                color = SiteVoiceTheme.colors.warning
                            )
                            Text(
                                text = "${content.chuaPhanLoai.size} mục",
                                fontSize = TextSizes.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = SiteVoiceTheme.colors.warning
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = stringResource(id = R.string.report_unclassified_hint),
                            fontSize = TextSizes.caption,
                            color = SiteVoiceTheme.colors.onWarningContainer
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))
                        content.chuaPhanLoai.forEach { item ->
                            Text(
                                text = "• $item",
                                fontSize = TextSizes.body,
                                color = SiteVoiceTheme.colors.onWarningContainer,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Các nút hành động chính
            if (!isApproved) {
                // Nút Phê duyệt 1 lần theo quyết định 0006
                Button(
                    onClick = onApproveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(id = R.string.report_btn_approve),
                        fontSize = TextSizes.subtitle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedButton(
                        onClick = onResynthesizeClick,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = "🔄 " + stringResource(id = R.string.report_btn_resynthesize), fontSize = TextSizes.caption)
                    }

                    OutlinedButton(
                        onClick = onBackToEditClick,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = stringResource(id = R.string.report_btn_back_to_edit), fontSize = TextSizes.caption)
                    }
                }
            } else {
                // Trạng thái đã duyệt: Nút Xuất Doc & Mở Doc
                val docUrl = reportEntity.docUrl
                if (!docUrl.isNullOrBlank()) {
                    // Đã xuất thành công
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.md),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Text(
                                text = "✓ " + stringResource(id = R.string.report_export_success),
                                fontSize = TextSizes.body,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = "Bản sao đã được tạo tại Google Drive cá nhân của bạn.",
                                fontSize = TextSizes.caption,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Button(
                                onClick = { onOpenDocClick?.invoke(docUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = stringResource(id = R.string.report_btn_open_doc),
                                    fontSize = TextSizes.body,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                // Nút xuất Google Doc (hoặc xuất lại)
                Button(
                    onClick = onExportDocClick,
                    enabled = !isExporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (docUrl.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(end = Spacing.sm)
                        )
                        Text(
                            text = stringResource(id = R.string.report_exporting_loading),
                            fontSize = TextSizes.body,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        val exportBtnText = if (docUrl.isNullOrBlank()) {
                            "📄 " + stringResource(id = R.string.report_btn_export_doc)
                        } else {
                            "🔄 Xuất lại bản Google Doc mới"
                        }
                        Text(
                            text = exportBtnText,
                            fontSize = TextSizes.subtitle,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedButton(
                    onClick = onResynthesizeClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(text = stringResource(id = R.string.report_edit_again), fontSize = TextSizes.caption)
                }
            }

            // Khối Chế độ Nhà phát triển: Xem & Sửa dữ liệu JSON do LLM trả về
            if (isDeveloperMode) {
                Spacer(modifier = Modifier.height(Spacing.lg))

                var rawJsonText by remember(reportEntity.contentJson) {
                    val formatted = try {
                        JSONObject(reportEntity.contentJson).toString(2)
                    } catch (_: Exception) {
                        reportEntity.contentJson
                    }
                    mutableStateOf(formatted)
                }
                var isEditingRaw by remember { mutableStateOf(false) }
                var saveMessage by remember { mutableStateOf<String?>(null) }
                val clipboardManager = LocalClipboardManager.current

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛠️ Dữ liệu JSON do LLM trả về:",
                                fontSize = TextSizes.body,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(rawJsonText))
                                    saveMessage = "Đã sao chép JSON vào bộ nhớ tạm!"
                                }
                            ) {
                                Text("📋 Sao chép", fontSize = TextSizes.caption, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Text(
                            text = "Dữ liệu JSON nguyên bản bóc tách từ ghi chú. Bạn có thể sửa trực tiếp rồi bấm Lưu.",
                            fontSize = TextSizes.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        OutlinedTextField(
                            value = rawJsonText,
                            onValueChange = {
                                rawJsonText = it
                                isEditingRaw = true
                                saveMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 8,
                            maxLines = 18,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = TextSizes.caption,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        if (isEditingRaw) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Button(
                                onClick = {
                                    onUpdateRawJson?.invoke(rawJsonText)
                                    isEditingRaw = false
                                    saveMessage = "Đã cập nhật JSON vào báo cáo!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("💾 Lưu sửa đổi JSON vào Báo cáo", fontSize = TextSizes.caption, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }

                        saveMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = "✓ $msg",
                                fontSize = TextSizes.caption,
                                color = SiteVoiceTheme.colors.success,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, bg, textCol, borderCol) = when (status) {
        "APPROVED" -> Quadruple(stringResource(id = R.string.report_status_approved), SiteVoiceTheme.colors.successContainer, SiteVoiceTheme.colors.success, MaterialTheme.colorScheme.outlineVariant)
        "EXPORTED" -> Quadruple(stringResource(id = R.string.report_status_exported), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.outlineVariant)
        else -> Quadruple(stringResource(id = R.string.report_status_draft), SiteVoiceTheme.colors.warningContainer, SiteVoiceTheme.colors.warning, MaterialTheme.colorScheme.outlineVariant)
    }

    Box(
        modifier = Modifier
            .background(bg, MaterialTheme.shapes.small)
            .border(1.dp, borderCol, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        Text(
            text = label,
            fontSize = TextSizes.caption,
            fontWeight = FontWeight.Bold,
            color = textCol
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ReportSectionList(
    title: String,
    badgeText: String,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = TextSizes.body,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = badgeText,
                    fontSize = TextSizes.caption,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            if (items.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.report_empty_field),
                    fontSize = TextSizes.body,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = TextSizes.body,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(14.dp)
                        )
                        Text(
                            text = item,
                            fontSize = TextSizes.body,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = TextSizes.bodyLine
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OpinionRow(label: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = TextSizes.caption,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = content.ifBlank { stringResource(id = R.string.report_empty_field) },
            fontSize = TextSizes.body,
            color = if (content.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = if (content.isNotBlank()) androidx.compose.ui.text.font.FontStyle.Normal else androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}
