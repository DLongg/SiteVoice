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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.template.TemplateSchema
import androidx.compose.material3.MaterialTheme
import com.sitevoice.app.ui.theme.SiteVoiceTheme
import com.sitevoice.app.ui.theme.TextSizes
import com.sitevoice.app.ui.theme.Spacing

@Composable
fun TemplateLibraryScreen(
    templates: List<TemplateItemEntity>,
    onClose: () -> Unit,
    onActivateTemplate: (String) -> Unit,
    onCreateNewTemplate: () -> Unit,
    onEditTemplate: (TemplateItemEntity) -> Unit,
    onDuplicateTemplate: (TemplateItemEntity) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onAutoSetupFromDoc: (String) -> Unit = {}
) {
    var templateToDelete by remember { mutableStateOf<TemplateItemEntity?>(null) }
    var docUrlInput by remember { mutableStateOf("") }
    var docUrlError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.lg)
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
                    fontSize = TextSizes.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.template_library_subtitle),
                    fontSize = TextSizes.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Text(text = "✕", fontSize = TextSizes.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Khối Tạo Mẫu Tự Động từ Google Doc (Gọi Docs API + LLM API trước)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.medium)
                .padding(Spacing.lg)
        ) {
            Text(
                text = stringResource(id = R.string.template_library_auto_setup_card_title),
                fontSize = TextSizes.subtitle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(id = R.string.template_library_auto_setup_card_desc),
                fontSize = TextSizes.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = docUrlInput,
                onValueChange = {
                    docUrlInput = it
                    docUrlError = null
                },
                placeholder = { Text("Dán link hoặc ID Google Doc biểu mẫu vào đây...", fontSize = TextSizes.caption) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            docUrlError?.let { err ->
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(text = "⚠️ $err", fontSize = TextSizes.caption, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Button(
                    onClick = {
                        if (docUrlInput.isBlank()) {
                            docUrlError = "Vui lòng dán link hoặc ID Google Doc trước khi quét."
                        } else {
                            onAutoSetupFromDoc(docUrlInput.trim())
                        }
                    },
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(id = R.string.template_library_auto_setup_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = TextSizes.caption,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                OutlinedButton(
                    onClick = onCreateNewTemplate,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(id = R.string.template_btn_create_new),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = TextSizes.caption,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Danh sách mẫu
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTemplate(target.id)
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isActive) 1.5.dp else 1.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .border(borderWidth, borderColor, shape = MaterialTheme.shapes.medium)
            .padding(Spacing.lg)
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
                    fontSize = TextSizes.subtitle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (template.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = template.description,
                        fontSize = TextSizes.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            // Badge Trạng thái
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .background(SiteVoiceTheme.colors.successContainer, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    ) {
                        Text(
                            text = "✓ " + stringResource(id = R.string.template_badge_active),
                            fontSize = TextSizes.caption,
                            fontWeight = FontWeight.Bold,
                            color = SiteVoiceTheme.colors.success
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (template.isBuiltIn) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                ) {
                    Text(
                        text = stringResource(
                            id = if (template.isBuiltIn) R.string.template_badge_builtin else R.string.template_badge_custom
                        ),
                        fontSize = TextSizes.caption,
                        fontWeight = FontWeight.Medium,
                        color = if (template.isBuiltIn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Thống kê các trường
        schema?.let { s ->
            val fieldCount = s.summaryFields.size
            val maxPages = s.maxPages
            Text(
                text = "📄 Giới hạn: $maxPages trang • Có $fieldCount trường tổng hợp: ${s.summaryFields.keys.take(3).joinToString(", ")}${if (fieldCount > 3) "..." else ""}",
                fontSize = TextSizes.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Hàng nút thao tác
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isActive) {
                Button(
                    onClick = onActivate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.template_btn_activate),
                        fontSize = TextSizes.caption,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
            }

            OutlinedButton(
                onClick = onEdit,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.template_btn_edit),
                    fontSize = TextSizes.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            OutlinedButton(
                onClick = onDuplicate,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.template_btn_duplicate),
                    fontSize = TextSizes.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!template.isBuiltIn) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(text = "🗑️", fontSize = TextSizes.body)
                }
            }
        }
    }
}
