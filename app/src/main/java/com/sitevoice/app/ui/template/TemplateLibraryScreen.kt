package com.sitevoice.app.ui.template

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.template.TemplateSchema

@Composable
fun TemplateLibraryScreen(
    templates: List<TemplateItemEntity>,
    onClose: () -> Unit,
    onActivateTemplate: (String) -> Unit,
    onCreateNewTemplate: () -> Unit,
    onEditTemplate: (TemplateItemEntity) -> Unit,
    onDuplicateTemplate: (TemplateItemEntity) -> Unit,
    onDeleteTemplate: (String) -> Unit
) {
    var templateToDelete by remember { mutableStateOf<TemplateItemEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.template_library_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = stringResource(id = R.string.template_library_subtitle),
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }
            IconButton(onClick = onClose) {
                Text(text = "✕", fontSize = 18.sp, color = Color(0xFF475569))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nút Tạo Mẫu Mới
        Button(
            onClick = onCreateNewTemplate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.template_btn_create_new),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Danh sách mẫu
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates, key = { it.id }) { template ->
                TemplateCardItem(
                    template = template,
                    onActivate = { onActivateTemplate(template.id) },
                    onEdit = { onEditTemplate(template) },
                    onDuplicate = { onDuplicateTemplate(template) },
                    onDelete = { templateToDelete = template }
                )
            }
        }
    }

    // Dialog xác nhận xóa mẫu
    templateToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = {
                Text(
                    text = stringResource(id = R.string.template_delete_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.template_delete_confirm_msg, target.name),
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTemplate(target.id)
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(text = stringResource(id = R.string.notes_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text(text = stringResource(id = R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun TemplateCardItem(
    template: TemplateItemEntity,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val schema = remember(template.schemaJson) {
        try {
            TemplateSchema.fromJson(template.schemaJson)
        } catch (_: Exception) {
            null
        }
    }

    val isActive = template.isActive
    val borderColor = if (isActive) Color(0xFF2563EB) else Color(0xFFE2E8F0)
    val borderWidth = if (isActive) 1.5.dp else 1.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(10.dp))
            .border(borderWidth, borderColor, shape = RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        // Hàng trên: Tên + Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                if (template.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = template.description,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Badge Trạng thái
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✓ " + stringResource(id = R.string.template_badge_active),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (template.isBuiltIn) Color(0xFFF1F5F9) else Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = if (template.isBuiltIn) R.string.template_badge_builtin else R.string.template_badge_custom
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (template.isBuiltIn) Color(0xFF475569) else Color(0xFF2563EB)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Thống kê các trường
        schema?.let { s ->
            val fieldCount = s.summaryFields.size
            val maxPages = s.maxPages
            Text(
                text = "📄 Giới hạn: $maxPages trang • Có $fieldCount trường tổng hợp: ${s.summaryFields.keys.take(3).joinToString(", ")}${if (fieldCount > 3) "..." else ""}",
                fontSize = 12.sp,
                color = Color(0xFF475569)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hàng nút thao tác
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isActive) {
                Button(
                    onClick = onActivate,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.template_btn_activate),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            OutlinedButton(
                onClick = onEdit,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.template_btn_edit),
                    fontSize = 12.sp,
                    color = Color(0xFF334155)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onDuplicate,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.template_btn_duplicate),
                    fontSize = 12.sp,
                    color = Color(0xFF334155)
                )
            }

            if (!template.isBuiltIn) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(text = "🗑️", fontSize = 14.sp)
                }
            }
        }
    }
}
