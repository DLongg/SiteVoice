package com.sitevoice.app.ui.template

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormSetupAnalysis

@Composable
fun FormSetupDialog(
    analysis: FormSetupAnalysis?,
    isLoading: Boolean,
    errorMessage: String?,
    onConfirmSetup: (templateName: String, approvedCandidates: List<FormCandidate>) -> Unit,
    onDismiss: () -> Unit
) {
    var templateName by remember(analysis) {
        mutableStateOf(analysis?.originalDocTitle?.replace("[SiteVoice Mẫu] ", "") ?: "Mẫu tự động")
    }

    // Danh sách ứng viên có thể bật/tắt hoặc chỉnh sửa
    var candidateList by remember(analysis) {
        mutableStateOf(analysis?.candidates.orEmpty())
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
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
                    Column {
                        Text(
                            text = "Thiết lập Form Tự động",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tự động phát hiện dòng chấm, ô tích và chèn placeholder",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isLoading) {
                        Text("✕", fontSize = 16.sp, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "2. Các vị trí chỗ điền phát hiện (${candidateList.count { !it.boQua }} chỗ):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidateList) { item ->
                            val isIncluded = !item.boQua

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isIncluded) Color(0xFFF8FAFC) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isIncluded) Color(0xFFCBD5E1) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isIncluded,
                                        onCheckedChange = { checked ->
                                            candidateList = candidateList.map {
                                                if (it.id == item.id) it.copy(boQua = !checked) else it
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "{{${item.ma}}}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isIncluded) Color(0xFF1D4ED8) else Color(0xFF94A3B8)
                                            )
                                            Text(
                                                text = item.loaiGoiY,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                modifier = Modifier
                                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = item.ten,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isIncluded) Color(0xFF1E293B) else Color(0xFF94A3B8)
                                        )

                                        if (item.truoc.isNotBlank() || item.sau.isNotBlank()) {
                                            Text(
                                                text = "Ngữ cảnh: \"${item.truoc}...${item.sau}\"",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
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
