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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.DailyReportEntity
import com.sitevoice.app.template.DailyReportContent

@Composable
fun DailyReportPreviewScreen(
    currentDate: String,
    reportEntity: DailyReportEntity?,
    dailyInput: DailyInputEntity?,
    isSynthesizing: Boolean,
    isExporting: Boolean = false,
    activeTemplateName: String? = null,
    onApproveClick: () -> Unit,
    onResynthesizeClick: () -> Unit,
    onBackToEditClick: () -> Unit,
    onExportDocClick: () -> Unit,
    onOpenDocClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        if (isSynthesizing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF0F172A),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(id = R.string.report_synthesizing_loading),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.report_synthesizing_budget_note),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
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
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Chưa có bản tổng hợp nào cho ngày $currentDate",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onResynthesizeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = stringResource(id = R.string.btn_generate_report))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onBackToEditClick,
                        shape = RoundedCornerShape(8.dp)
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Thanh điều hướng trên cùng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackToEditClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "← " + stringResource(id = R.string.report_btn_back_to_edit), fontSize = 12.sp)
                }

                // Status Chip
                StatusBadge(status = reportEntity.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tiêu đề
            Text(
                text = stringResource(id = R.string.report_preview_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            val templateDisplayName = activeTemplateName ?: (if (reportEntity.templateId == "nhat-ky-ca-nhan") "Nhật ký cá nhân" else "Nhật ký thi công công trường")
            Text(
                text = "Ngày: $currentDate • Mẫu: $templateDisplayName",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            // Banner phê duyệt nếu đã duyệt
            if (isApproved) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.report_approved_banner),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isConstruction = reportEntity.templateId == "nhat-ky-cong-truong" || reportEntity.templateId.isBlank()

            if (isConstruction) {
                // Khối 1: Thông tin hiện trường (Thời tiết, Nhân lực, Máy móc)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(id = R.string.report_section_general),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val weatherText = buildString {
                            val morning = if (dailyInput?.weatherMorning == "mua") "🌧️ Mưa" else "☀️ Nắng"
                            val afternoon = if (dailyInput?.weatherAfternoon == "mua") "🌧️ Mưa" else "☀️ Nắng"
                            val evening = if (dailyInput?.weatherEvening == "mua") "🌧️ Mưa" else "🌙 Bình thường"
                            append("Sáng: $morning  •  Chiều: $afternoon  •  Tối: $evening")
                        }
                        Text(text = "Thời tiết: $weatherText", fontSize = 13.sp, color = Color(0xFF334155))

                        Spacer(modifier = Modifier.height(4.dp))
                        val bch = dailyInput?.commandStaffCount ?: 0
                        val cn = dailyInput?.workerCount ?: 0
                        Text(text = "Lực lượng: Ban chỉ huy: $bch người  |  Công nhân: $cn người", fontSize = 13.sp, color = Color(0xFF334155))

                        Spacer(modifier = Modifier.height(4.dp))
                        val eq = dailyInput?.equipmentList?.ifBlank { "Không ghi nhận máy móc chính" } ?: "Không ghi nhận máy móc chính"
                        Text(text = "Thiết bị: $eq", fontSize = 13.sp, color = Color(0xFF334155))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Khối 2: Công việc thi công (Ngân sách: ≤ 15 dòng)
                ReportSectionList(
                    title = stringResource(id = R.string.report_section_construction),
                    badgeText = "${content.congViecThiCong.size}/15 dòng",
                    items = content.congViecThiCong
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Khối 3: Nghiệm thu công việc (Ngân sách: ≤ 6 dòng)
                ReportSectionList(
                    title = stringResource(id = R.string.report_section_inspection),
                    badgeText = "${content.nghiemThu.size}/6 dòng",
                    items = content.nghiemThu
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Khối 4: An toàn lao động & VSMT (Ngân sách: ≤ 3 dòng)
                ReportSectionList(
                    title = stringResource(id = R.string.report_section_safety),
                    badgeText = "${content.anToan.size}/3 dòng",
                    items = content.anToan
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Khối 5: Ý kiến các bên (Tối đa 200 ký tự mỗi bên)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(id = R.string.report_section_opinions),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OpinionRow(label = stringResource(id = R.string.report_opinion_cdt), content = content.yKienCdt)
                        Spacer(modifier = Modifier.height(8.dp))
                        OpinionRow(label = stringResource(id = R.string.report_opinion_tvgs), content = content.yKienTvgs)
                        Spacer(modifier = Modifier.height(8.dp))
                        OpinionRow(label = stringResource(id = R.string.report_opinion_contractor), content = content.yKienNhaThau)
                    }
                }
            } else {
                // Hiển thị động theo các trường của Mẫu cá nhân / Mẫu tự tạo
                val dynamicFields = content.allFields.filterKeys { it != "rawJson" && it != "chua_phan_loai" }
                dynamicFields.forEach { (key, value) ->
                    val readableTitle = key.replace("_", " ").replaceFirstChar { it.uppercase() }
                    if (value is List<*>) {
                        val stringList = value.mapNotNull { it?.toString() }
                        ReportSectionList(
                            title = readableTitle,
                            badgeText = "${stringList.size} mục",
                            items = stringList
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = readableTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = value.toString().ifBlank { stringResource(id = R.string.report_empty_field) },
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            // Khối 6: Ghi chú chưa phân loại (nếu có)
            if (content.chuaPhanLoai.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ " + stringResource(id = R.string.report_section_unclassified),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "${content.chuaPhanLoai.size} mục",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.report_unclassified_hint),
                            fontSize = 11.sp,
                            color = Color(0xFF92400E)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        content.chuaPhanLoai.forEach { item ->
                            Text(
                                text = "• $item",
                                fontSize = 13.sp,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Các nút hành động chính
            if (!isApproved) {
                // Nút Phê duyệt 1 lần theo quyết định 0006
                Button(
                    onClick = onApproveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.report_btn_approve),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onResynthesizeClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "🔄 " + stringResource(id = R.string.report_btn_resynthesize), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onBackToEditClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = stringResource(id = R.string.report_btn_back_to_edit), fontSize = 12.sp)
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
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "✓ " + stringResource(id = R.string.report_export_success),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bản sao đã được tạo tại Google Drive cá nhân của bạn.",
                                fontSize = 12.sp,
                                color = Color(0xFF3B82F6)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onOpenDocClick?.invoke(docUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.report_btn_open_doc),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
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
                    colors = ButtonDefaults.buttonColors(containerColor = if (docUrl.isNullOrBlank()) Color(0xFF2563EB) else Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.report_exporting_loading),
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    } else {
                        val exportBtnText = if (docUrl.isNullOrBlank()) {
                            "📄 " + stringResource(id = R.string.report_btn_export_doc)
                        } else {
                            "🔄 Xuất lại bản Google Doc mới"
                        }
                        Text(
                            text = exportBtnText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onResynthesizeClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = stringResource(id = R.string.report_edit_again), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, bg, textCol, borderCol) = when (status) {
        "APPROVED" -> Quadruple(stringResource(id = R.string.report_status_approved), Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFF86EFAC))
        "EXPORTED" -> Quadruple(stringResource(id = R.string.report_status_exported), Color(0xFFDBEAFE), Color(0xFF1D4ED8), Color(0xFF93C5FD))
        else -> Quadruple(stringResource(id = R.string.report_status_draft), Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFFCD34D))
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .border(1.dp, borderCol, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.report_empty_field),
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF94A3B8)
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.width(14.dp)
                        )
                        Text(
                            text = item,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B),
                            lineHeight = 18.sp
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
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = content.ifBlank { stringResource(id = R.string.report_empty_field) },
            fontSize = 13.sp,
            color = if (content.isNotBlank()) Color(0xFF1E293B) else Color(0xFF94A3B8),
            fontStyle = if (content.isNotBlank()) androidx.compose.ui.text.font.FontStyle.Normal else androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}
