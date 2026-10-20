package com.sitevoice.app.ui.template

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import org.json.JSONArray
import org.json.JSONObject

data class FieldEditorState(
    var key: String,
    var type: String = "danh_sach", // "danh_sach" hoặc "van_ban"
    var maxLines: Int = 10,
    var maxCharsPerLine: Int = 100
)

@Composable
fun TemplateEditorScreen(
    template: TemplateItemEntity?,
    onClose: () -> Unit,
    onSaveTemplate: (TemplateItemEntity) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var docTemplateUrl by remember { mutableStateOf(template?.docTemplateUrl ?: "") }
    var systemPrompt by remember {
        mutableStateOf(
            template?.systemPrompt ?: """Bạn là trợ lý tổng hợp thông tin chuyên nghiệp.
Từ các ghi chú âm thanh trong ngày, hãy viết bản tổng hợp ngắn gọn, mạch lạc và chuẩn xác.

Quy tắc bắt buộc:
1. CHỈ dùng thông tin có trong ghi chú. Không suy đoán.
2. Mục không có dữ liệu: trả về mảng rỗng [] hoặc chuỗi rỗng "".
3. Tuân thủ giới hạn số dòng và số ký tự của từng trường.
4. Trả về CHỈ JSON theo đúng định dạng được yêu cầu."""
        )
    }

    // Danh sách các trường tổng hợp
    val fields = remember {
        mutableStateListOf<FieldEditorState>().apply {
            if (template != null) {
                try {
                    val schema = TemplateSchema.fromJson(template.schemaJson)
                    schema.summaryFields.forEach { (key, config) ->
                        add(
                            FieldEditorState(
                                key = key,
                                type = config.type,
                                maxLines = if (config.maxLines > 0) config.maxLines else 10,
                                maxCharsPerLine = config.maxCharsPerLine
                            )
                        )
                    }
                } catch (_: Exception) {
                    add(FieldEditorState("cong_viec", "danh_sach", 10, 100))
                }
            } else {
                add(FieldEditorState("cong_viec_chinh", "danh_sach", 12, 100))
                add(FieldEditorState("van_de_ton_dong", "danh_sach", 8, 100))
                add(FieldEditorState("ghi_chu_khac", "van_ban", 5, 100))
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // TopBar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Text(text = "←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(
                        id = if (template == null) R.string.template_editor_create_title else R.string.template_editor_edit_title
                    ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Form nội dung cuộn
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Phần 1: Thông tin cơ bản
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Thông tin cơ bản",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.template_field_name)) },
                    placeholder = { Text(stringResource(id = R.string.template_field_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.template_field_desc)) },
                    placeholder = { Text(stringResource(id = R.string.template_field_desc_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = docTemplateUrl,
                    onValueChange = { docTemplateUrl = it },
                    label = { Text(stringResource(id = R.string.template_field_doc_url)) },
                    placeholder = { Text(stringResource(id = R.string.template_field_doc_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Phần 2: Căn chỉnh trường placeholder & Giới hạn dòng/ký tự
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. " + stringResource(id = R.string.template_section_fields),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = stringResource(id = R.string.template_section_fields_hint),
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                // Danh sách trường
                fields.forEachIndexed { index, fieldState ->
                    FieldItemEditor(
                        fieldState = fieldState,
                        onDelete = {
                            if (fields.size > 1) {
                                fields.removeAt(index)
                            }
                        },
                        canDelete = fields.size > 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        fields.add(FieldEditorState("truong_moi_${fields.size + 1}", "danh_sach", 8, 100))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(text = stringResource(id = R.string.template_btn_add_field), fontSize = 13.sp)
                }
            }

            // Phần 3: Prompt AI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "3. " + stringResource(id = R.string.template_section_prompt),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = stringResource(id = R.string.template_section_prompt_hint),
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 12
                )
            }

            // Báo lỗi nếu có
            errorMessage?.let { msg ->
                Text(
                    text = "⚠️ $msg",
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nút Lưu Mẫu
        Button(
            onClick = {
                if (name.isBlank()) {
                    errorMessage = "Vui lòng nhập tên mẫu tài liệu"
                    return@Button
                }
                if (fields.isEmpty()) {
                    errorMessage = "Cần ít nhất một trường placeholder"
                    return@Button
                }

                // Sinh schemaJson từ fields
                val schemaObj = JSONObject()
                val id = template?.id ?: ("custom_" + System.currentTimeMillis())
                schemaObj.put("id", id)
                schemaObj.put("ten", name)
                schemaObj.put("gom_theo", "ngay")
                schemaObj.put("gioi_han_trang", 2)

                val categoriesArray = JSONArray()
                fields.forEach { categoriesArray.put(it.key) }
                categoriesArray.put("chua_phan_loai")
                schemaObj.put("muc_ghi_chu", categoriesArray)

                val summaryFieldsObj = JSONObject()
                fields.forEach { f ->
                    val fObj = JSONObject()
                    fObj.put("kieu", f.type)
                    fObj.put("toi_da_dong", f.maxLines)
                    fObj.put("ky_tu_moi_dong", f.maxCharsPerLine)
                    val src = JSONArray()
                    src.put(f.key)
                    fObj.put("muc_nguon", src)
                    summaryFieldsObj.put(f.key, fObj)
                }
                schemaObj.put("truong_tong_hop", summaryFieldsObj)

                val updated = TemplateItemEntity(
                    id = id,
                    name = name.trim(),
                    description = description.trim(),
                    docTemplateUrl = docTemplateUrl.trim(),
                    systemPrompt = systemPrompt.trim(),
                    schemaJson = schemaObj.toString(2),
                    isBuiltIn = template?.isBuiltIn ?: false,
                    isActive = template?.isActive ?: false,
                    updatedAt = System.currentTimeMillis()
                )

                onSaveTemplate(updated)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.template_btn_save_template),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun FieldItemEditor(
    fieldState: FieldEditorState,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    var key by remember { mutableStateOf(fieldState.key) }
    var type by remember { mutableStateOf(fieldState.type) }
    var maxLines by remember { mutableIntStateOf(fieldState.maxLines) }
    var maxCharsPerLine by remember { mutableIntStateOf(fieldState.maxCharsPerLine) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFCBD5E1), shape = RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hàng 1: Mã placeholder + Nút xóa
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                    fieldState.key = it
                },
                label = { Text("Mã placeholder {{...}}", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Text(text = "🗑️", fontSize = 13.sp)
                }
            }
        }

        // Hàng 2: Chọn kiểu dữ liệu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Kiểu:", fontSize = 12.sp, color = Color(0xFF475569))

            FilterChip(
                selected = type == "danh_sach",
                onClick = {
                    type = "danh_sach"
                    fieldState.type = "danh_sach"
                },
                label = { Text(stringResource(id = R.string.template_field_type_list), fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2563EB),
                    selectedLabelColor = Color.White
                )
            )

            FilterChip(
                selected = type == "van_ban",
                onClick = {
                    type = "van_ban"
                    fieldState.type = "van_ban"
                },
                label = { Text(stringResource(id = R.string.template_field_type_text), fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2563EB),
                    selectedLabelColor = Color.White
                )
            )
        }

        // Hàng 3: Căn chỉnh số dòng và ký tự
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Số dòng tối đa
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Dòng:", fontSize = 12.sp, color = Color(0xFF475569))
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (maxLines > 1) {
                            maxLines--
                            fieldState.maxLines = maxLines
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(text = "−", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$maxLines",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = {
                        maxLines++
                        fieldState.maxLines = maxLines
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(text = "+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Ký tự / dòng
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Ký tự/dòng:", fontSize = 12.sp, color = Color(0xFF475569))
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (maxCharsPerLine > 30) {
                            maxCharsPerLine -= 10
                            fieldState.maxCharsPerLine = maxCharsPerLine
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(text = "−", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$maxCharsPerLine",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = {
                        maxCharsPerLine += 10
                        fieldState.maxCharsPerLine = maxCharsPerLine
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(text = "+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
