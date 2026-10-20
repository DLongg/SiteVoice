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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormSetupAnalysis

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
    var templateName by remember(analysis) {
        mutableStateOf(analysis?.originalDocTitle?.replace("[SiteVoice Mẫu] ", "") ?: "Mẫu tự động")
    }

    // Danh sách ứng viên có thể bật/tắt hoặc chỉnh sửa tay trực tiếp
    var candidateList by remember(analysis) {
        mutableStateOf(analysis?.candidates.orEmpty())
    }

    // ID của candidate đang được mở rộng chỉnh sửa (null nếu không có)
    var editingCandidateId by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Thiết lập Form Tự động",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Nhận diện chỗ điền & cho phép tinh chỉnh trực tiếp",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isLoading) {
                        Text("✕", fontSize = 18.sp, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                                color = Color(0xFF2563EB),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Đang quét cấu trúc & nhận diện chỗ điền...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "Tạo bản sao an toàn trên Drive và phân loại vị trí",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Không thể phân tích form:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = errorMessage, fontSize = 13.sp, color = Color(0xFF7F1D1D))
                            }
                        }
                    }
                } else if (analysis != null) {
                    // Danh sách kết quả phân tích
                    Text(
                        text = "1. Tên Mẫu hiển thị:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Khối Bản sao an toàn & Mở Google Docs gõ sửa
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bản sao an toàn trên Google Drive:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = analysis.originalDocTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(analysis.workingDocUrl))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "📄 Mở & gõ sửa trên Docs ↗",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = onRescan,
                                    enabled = !isLoading,
                                    modifier = Modifier.weight(0.7f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🔄 Quét lại",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💡 File gốc giữ nguyên. Bạn có thể mở bản sao để gõ sửa tùy ý trên Google Docs rồi bấm 'Quét lại' để cập nhật.",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeCount = candidateList.count { !it.boQua }
                    val skippedCount = candidateList.size - activeCount

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Chỗ điền phát hiện ($activeCount chèn, $skippedCount bỏ qua):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Text(
                        text = "Chạm vào thẻ hoặc nút 'Sửa' để chỉnh sửa tay mã thẻ, tên, loại và nguồn.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Hủy", color = Color(0xFF475569))
                        }

                        Button(
                            onClick = {
                                onConfirmSetup(templateName, candidateList)
                            },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("✓ Chèn thẻ & Tạo Mẫu", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
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
        shape = RoundedCornerShape(8.dp),
        color = if (isExpanded) {
            Color(0xFFF8FAFC)
        } else if (isIncluded) {
            Color(0xFFFFFFFF)
        } else {
            Color(0xFFF1F5F9)
        },
        border = androidx.compose.foundation.BorderStroke(
            if (isExpanded) 1.5.dp else 1.dp,
            if (isExpanded) Color(0xFF3B82F6) else if (isIncluded) Color(0xFFCBD5E1) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
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

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "{{${candidate.ma}}}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isIncluded) Color(0xFF1D4ED8) else Color(0xFF94A3B8)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Badge Loại
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = formatLoaiLabel(candidate.loai),
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                // Badge Nguồn
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFBFDBFE))
                                ) {
                                    Text(
                                        text = formatNguonLabel(candidate.nguon),
                                        fontSize = 10.sp,
                                        color = Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = candidate.ten.ifBlank { "(Chưa đặt tên nhãn)" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isIncluded) Color(0xFF1E293B) else Color(0xFF94A3B8)
                        )

                        if (candidate.truoc.isNotBlank() || candidate.sau.isNotBlank()) {
                            Text(
                                text = "Vị trí: \"${candidate.truoc}...${candidate.sau}\"",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Nút bấm sửa
                    TextButton(
                        onClick = onToggleExpand,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Sửa ✏️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", fontSize = 14.sp, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 1. Mã placeholder
                Text(
                    text = "Mã thẻ placeholder (ghi vào Doc dạng {{...}}):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = candidate.ma,
                    onValueChange = { input ->
                        val normalized = input.uppercase().replace(" ", "_")
                        onUpdate(candidate.copy(ma = normalized))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    trailingIcon = {
                        Text(
                            text = "{{${candidate.ma}}}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Tên mục
                Text(
                    text = "Tên mục hiển thị trong báo cáo:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = candidate.ten,
                    onValueChange = { onUpdate(candidate.copy(ten = it)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Kiểu dữ liệu (loai)
                Text(
                    text = "Kiểu dữ liệu:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LOAI_OPTIONS.forEach { (key, label) ->
                        val isSelected = candidate.loai.equals(key, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable {
                                onUpdate(candidate.copy(loai = key))
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Nguồn dữ liệu (nguon)
                Text(
                    text = "Nguồn dữ liệu bóc tách:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NGUON_OPTIONS.forEach { (key, label) ->
                        val isSelected = candidate.nguon.equals(key, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF1E40AF) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable {
                                onUpdate(candidate.copy(nguon = key))
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Thao tác phụ: Bỏ qua / Áp dụng & Nút Xong
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onUpdate(candidate.copy(boQua = !candidate.boQua))
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (candidate.boQua) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                    ) {
                        Text(
                            text = if (candidate.boQua) "✓ Khôi phục vị trí này" else "✕ Bỏ qua vị trí này",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onToggleExpand,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(
                            text = "✓ Xong",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
