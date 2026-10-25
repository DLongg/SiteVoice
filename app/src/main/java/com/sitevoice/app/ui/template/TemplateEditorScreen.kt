package com.sitevoice.app.ui.template

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormLlmDescriber
import com.sitevoice.app.template.TemplateSchema
import org.json.JSONArray
import org.json.JSONObject

data class FieldEditorState(
    var key: String,
    var label: String = "",
    var type: String = "danh_sach", // "danh_sach", "van_ban", "so", "ngay", "o_tich"
    var source: String = "thi_cong",
    var maxLines: Int = 10,
    var maxCharsPerLine: Int = 100,
    var startIndex: Int = -1,
    var endIndex: Int = -1,
    var tieuDeGanNhat: String = "",
    var truoc: String = "",
    var sau: String = "",
    var tinHieu: String = "dong_cham"
)

private val EDITOR_SOURCE_OPTIONS = listOf(
    "thi_cong" to "Thi công",
    "nghiem_thu" to "Nghiệm thu",
    "an_toan" to "An toàn & VSMT",
    "nhap_tay" to "Nhập tay cuối ngày",
    "y_kien_cdt" to "Ý kiến CĐT",
    "y_kien_tvgs" to "Ý kiến TVGS",
    "y_kien_nha_thau" to "Ý kiến Nhà thầu"
)

@Composable
fun TemplateEditorScreen(
    template: TemplateItemEntity?,
    onClose: () -> Unit,
    onSaveTemplate: (TemplateItemEntity) -> Unit,
    onScanDocWithAi: ((String) -> Unit)? = null,
    onSyncFieldsFromDoc: ((String, (Result<List<FormCandidate>>) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(template?.name ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var docTemplateUrl by remember { mutableStateOf(template?.docTemplateUrl ?: "") }
    var systemPrompt by remember {
        mutableStateOf(
            template?.systemPrompt ?: """Bạn là thư ký kỹ thuật công trường xây dựng chuyên nghiệp tại Việt Nam.
Từ các ghi chú âm thanh trong ngày, hãy viết bản tổng hợp ngắn gọn, mạch lạc và chuẩn xác.

Quy tắc bắt buộc:
1. CHỈ dùng thông tin có trong ghi chú. Không suy đoán.
2. Mục không có dữ liệu: trả về mảng rỗng [] hoặc chuỗi rỗng "".
3. Tuân thủ giới hạn số dòng và số ký tự của từng trường.
4. Trả về CHỈ JSON theo đúng định dạng được yêu cầu."""
        )
    }

    // Danh sách các trường tổng hợp:
    // Khi tạo mẫu mới (template == null), KHÔNG viết cứng 3 trường giả; để trống để người dùng quét từ Doc qua AI hoặc tự thêm.
    val fields = remember {
        mutableStateListOf<FieldEditorState>().apply {
            if (template != null && template.schemaJson.isNotBlank()) {
                try {
                    val rawJson = JSONObject(template.schemaJson)
                    val summaryMap = rawJson.optJSONObject("truong_tong_hop")
                        ?: rawJson.optJSONObject("summary_fields")
                    val schema = TemplateSchema.fromJson(template.schemaJson)
                    schema.summaryFields.forEach { (key, config) ->
                        val rawFieldObj = summaryMap?.optJSONObject(key)
                        val label = rawFieldObj?.optString("label")?.ifBlank { key } ?: key
                        val src = config.sourceCategories.firstOrNull() ?: "thi_cong"
                        add(
                            FieldEditorState(
                                key = key,
                                label = label,
                                type = config.type,
                                source = src,
                                maxLines = if (config.maxLines > 0) config.maxLines else 8,
                                maxCharsPerLine = config.maxCharsPerLine,
                                startIndex = config.startIndex,
                                endIndex = config.endIndex,
                                tieuDeGanNhat = config.tieuDeGanNhat,
                                truoc = config.truoc,
                                sau = config.sau,
                                tinHieu = config.tinHieu.ifBlank { "dong_cham" }
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Fallback empty
                }
            }
        }
    }

    var isSyncingDoc by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    fun renderPromptFromCurrentFields(): String {
        val candidates = fields.mapIndexed { idx, f ->
            FormCandidate(
                id = idx + 1,
                startIndex = f.startIndex,
                endIndex = f.endIndex,
                truoc = f.truoc,
                sau = f.sau,
                tinHieu = f.tinHieu,
                tieuDeGanNhat = f.tieuDeGanNhat,
                loaiGoiY = f.type,
                ma = f.key.uppercase(),
                ten = f.label.ifBlank { f.key },
                loai = f.type,
                nguon = f.source,
                boQua = false
            )
        }
        return FormLlmDescriber.generateUnifiedPrompt(
            candidates = candidates,
            templateName = name.ifBlank { "Mẫu báo cáo" }
        )
    }

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
            // Phần 1: Thông tin cơ bản & Liên kết Google Doc
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Thông tin cơ bản & Bản mẫu Google Doc",
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
                    onValueChange = {
                        docTemplateUrl = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(id = R.string.template_field_doc_url)) },
                    placeholder = { Text(stringResource(id = R.string.template_field_doc_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Nút gọi AI tạo bản sao & lấy trường từ Google Doc
                if (onScanDocWithAi != null) {
                    Button(
                        onClick = {
                            if (docTemplateUrl.isBlank()) {
                                errorMessage = "Vui lòng dán Link hoặc ID Google Doc trước khi gọi AI quét trường."
                            } else {
                                onScanDocWithAi(docTemplateUrl.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.template_editor_scan_doc_btn),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Khối thao tác sửa vị trí trực tiếp bằng tay trên Google Doc & Đồng bộ ngược
                if (docTemplateUrl.isNotBlank()) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🛠️ Sửa vị trí thẻ {{MA}} trực tiếp bằng tay trên Google Doc:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Bạn có thể mở file Google Doc mẫu để tự gõ hoặc di chuyển các thẻ {{MA}} (VD: {{MAY_MOC}}, {{Y_KIEN_CDT}}) đến đúng vị trí mong muốn, sau đó bấm Đồng bộ lại.",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val url = if (docTemplateUrl.startsWith("http")) {
                                                docTemplateUrl.trim()
                                            } else {
                                                "https://docs.google.com/document/d/${docTemplateUrl.trim()}/edit"
                                            }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (e: Exception) {
                                            errorMessage = "Không thể mở trình duyệt: ${e.message}"
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.template_editor_open_doc_btn),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                }

                                if (onSyncFieldsFromDoc != null) {
                                    Button(
                                        onClick = {
                                            isSyncingDoc = true
                                            syncStatusMessage = null
                                            errorMessage = null
                                            onSyncFieldsFromDoc(docTemplateUrl.trim()) { result ->
                                                isSyncingDoc = false
                                                if (result.isSuccess) {
                                                    val scanned = result.getOrThrow().filter { !it.boQua }
                                                    fields.clear()
                                                    scanned.forEach { c ->
                                                        fields.add(
                                                            FieldEditorState(
                                                                key = c.ma.lowercase(),
                                                                label = c.ten.ifBlank { c.ma },
                                                                type = c.loai.ifBlank { "van_ban" },
                                                                source = c.nguon.ifBlank { "thi_cong" },
                                                                maxLines = if (c.loai == "danh_sach") 8 else 3,
                                                                maxCharsPerLine = 80,
                                                                startIndex = c.startIndex,
                                                                endIndex = c.endIndex,
                                                                tieuDeGanNhat = c.tieuDeGanNhat,
                                                                truoc = c.truoc,
                                                                sau = c.sau,
                                                                tinHieu = c.tinHieu
                                                            )
                                                        )
                                                    }
                                                    syncStatusMessage = "✓ Đã đồng bộ ${fields.size} trường và vị trí từ Google Doc!"
                                                } else {
                                                    errorMessage = result.exceptionOrNull()?.message ?: "Lỗi đồng bộ từ Google Doc"
                                                }
                                            }
                                        },
                                        enabled = !isSyncingDoc,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        if (isSyncingDoc) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = stringResource(id = R.string.template_editor_rescan_doc_btn),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            syncStatusMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }
            }

            // Phần 2: Căn chỉnh trường placeholder, Vị trí trong Doc & Giới hạn dòng/ký tự
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. " + stringResource(id = R.string.template_section_fields),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${fields.size} trường",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }

                Text(
                    text = "Mỗi trường hiển thị rõ mã {{MA}}, nguồn dữ liệu và vị trí neo trong Google Doc. Bạn có thể bấm '📍 Vị trí trong Doc' trên từng trường để xem/sửa trực tiếp.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                if (fields.isEmpty()) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.template_editor_empty_fields_title),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.template_editor_empty_fields_desc),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    fields.forEachIndexed { index, fieldState ->
                        FieldItemEditor(
                            fieldState = fieldState,
                            onDelete = {
                                fields.removeAt(index)
                            },
                            canDelete = true
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        fields.add(
                            FieldEditorState(
                                key = "truong_moi_${fields.size + 1}",
                                label = "Trường mới ${fields.size + 1}",
                                type = "danh_sach",
                                source = "thi_cong",
                                maxLines = 8,
                                maxCharsPerLine = 100
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(text = stringResource(id = R.string.template_btn_add_field), fontSize = 13.sp)
                }
            }

            // Phần 3: Prompt AI (Có nút Render từ các trường tổng hợp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. " + stringResource(id = R.string.template_section_prompt),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (fields.isEmpty()) {
                                errorMessage = "Cần ít nhất 1 trường ở mục 2 để Render Prompt."
                            } else {
                                errorMessage = null
                                systemPrompt = renderPromptFromCurrentFields()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.template_editor_render_prompt_btn),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = "Bấm '🔄 Render Prompt từ các trường tổng hợp' để tự động cập nhật chỉ thị AI khớp 1-1 với danh sách trường ở mục 2.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 14
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
                    errorMessage = "Cần ít nhất một trường placeholder (hãy bấm Quét từ Doc hoặc + Thêm trường mới)"
                    return@Button
                }

                // Sinh schemaJson từ fields (bảo toàn đầy đủ thông tin vị trí trong Doc)
                val schemaObj = JSONObject()
                val id = template?.id ?: ("custom_" + System.currentTimeMillis())
                schemaObj.put("id", id)
                schemaObj.put("ten", name)
                schemaObj.put("name", name)
                schemaObj.put("gom_theo", "ngay")
                schemaObj.put("group_by", "ngay")
                schemaObj.put("gioi_han_trang", 2)
                schemaObj.put("max_pages", 2)

                val categoriesArray = JSONArray()
                fields.forEach { categoriesArray.put(it.key) }
                categoriesArray.put("chua_phan_loai")
                schemaObj.put("muc_ghi_chu", categoriesArray)

                val summaryFieldsObj = JSONObject()
                val summaryFieldsArray = JSONArray()
                fields.forEach { f ->
                    val cleanKey = f.key.trim().lowercase()
                    val upperMa = cleanKey.uppercase()
                    val fObj = JSONObject().apply {
                        put("kieu", f.type)
                        put("type", f.type)
                        put("label", f.label.ifBlank { cleanKey })
                        put("placeholder", "{{$upperMa}}")
                        put("toi_da_dong", f.maxLines)
                        put("max_lines", f.maxLines)
                        put("ky_tu_moi_dong", f.maxCharsPerLine)
                        put("max_chars_per_line", f.maxCharsPerLine)
                        put("source", f.source)
                        val src = JSONArray()
                        src.put(f.source.ifBlank { cleanKey })
                        put("muc_nguon", src)
                        put("start_index", f.startIndex)
                        put("startIndex", f.startIndex)
                        put("end_index", f.endIndex)
                        put("endIndex", f.endIndex)
                        put("tieu_de_gan_nhat", f.tieuDeGanNhat)
                        put("tieuDeGanNhat", f.tieuDeGanNhat)
                        put("truoc", f.truoc)
                        put("sau", f.sau)
                        put("tin_hieu", f.tinHieu)
                        put("tinHieu", f.tinHieu)
                    }
                    summaryFieldsObj.put(cleanKey, fObj)

                    val arrObj = JSONObject().apply {
                        put("key", cleanKey)
                        put("label", f.label.ifBlank { cleanKey })
                        put("type", f.type)
                        put("placeholder", "{{$upperMa}}")
                        put("source", f.source)
                        put("max_lines", f.maxLines)
                        put("max_chars_per_line", f.maxCharsPerLine)
                        put("startIndex", f.startIndex)
                        put("endIndex", f.endIndex)
                        put("tieuDeGanNhat", f.tieuDeGanNhat)
                        put("truoc", f.truoc)
                        put("sau", f.sau)
                        put("tinHieu", f.tinHieu)
                    }
                    summaryFieldsArray.put(arrObj)
                }
                schemaObj.put("truong_tong_hop", summaryFieldsObj)
                schemaObj.put("summary_fields", summaryFieldsArray)

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
    var label by remember { mutableStateOf(fieldState.label) }
    var type by remember { mutableStateOf(fieldState.type) }
    var source by remember { mutableStateOf(fieldState.source) }
    var maxLines by remember { mutableIntStateOf(fieldState.maxLines) }
    var maxCharsPerLine by remember { mutableIntStateOf(fieldState.maxCharsPerLine) }
    var tieuDeGanNhat by remember { mutableStateOf(fieldState.tieuDeGanNhat) }
    var truoc by remember { mutableStateOf(fieldState.truoc) }
    var sau by remember { mutableStateOf(fieldState.sau) }
    var startIndex by remember { mutableIntStateOf(fieldState.startIndex) }
    var endIndex by remember { mutableIntStateOf(fieldState.endIndex) }
    var showPositionEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFCBD5E1), shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hàng 1: Mã placeholder + Tên mục + Nút xóa
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                    fieldState.key = it
                },
                label = { Text("Mã thẻ {{...}}", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = label,
                onValueChange = {
                    label = it
                    fieldState.label = it
                },
                label = { Text("Tên mục hiển thị", fontSize = 11.sp) },
                modifier = Modifier.weight(1.2f),
                singleLine = true
            )

            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Text(text = "🗑️", fontSize = 14.sp)
                }
            }
        }

        // Khung tóm tắt & nút mở sửa Vị trí trong bản mẫu Google Doc
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPositionEditor = !showPositionEditor }
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.field_position_section_title),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    TextButton(
                        onClick = { showPositionEditor = !showPositionEditor },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = if (showPositionEditor) "Thu gọn ▲" else "Sửa vị trí ✏️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                val posSummary = buildString {
                    if (tieuDeGanNhat.isNotBlank()) append("Đề mục: \"$tieuDeGanNhat\" • ")
                    if (truoc.isNotBlank() || sau.isNotBlank()) {
                        append("Trước: \"$truoc\" | Sau: \"$sau\" ")
                    } else {
                        append("(Chưa có ngữ cảnh trước/sau) ")
                    }
                    if (startIndex >= 0) {
                        append("[Ký tự #$startIndex..#$endIndex]")
                    }
                }
                Text(
                    text = posSummary,
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )

                if (showPositionEditor) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tieuDeGanNhat,
                        onValueChange = {
                            tieuDeGanNhat = it
                            fieldState.tieuDeGanNhat = it
                        },
                        label = { Text(stringResource(id = R.string.field_position_heading_label), fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = truoc,
                            onValueChange = {
                                truoc = it
                                fieldState.truoc = it
                            },
                            label = { Text(stringResource(id = R.string.field_position_before_label), fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = sau,
                            onValueChange = {
                                sau = it
                                fieldState.sau = it
                            },
                            label = { Text(stringResource(id = R.string.field_position_after_label), fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (startIndex >= 0) startIndex.toString() else "",
                            onValueChange = { str ->
                                val v = str.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: -1
                                startIndex = v
                                fieldState.startIndex = v
                            },
                            label = { Text(stringResource(id = R.string.field_position_start_index), fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = if (endIndex >= 0) endIndex.toString() else "",
                            onValueChange = { str ->
                                val v = str.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: -1
                                endIndex = v
                                fieldState.endIndex = v
                            },
                            label = { Text(stringResource(id = R.string.field_position_end_index), fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Hàng 2: Chọn kiểu dữ liệu & Nguồn dữ liệu
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

        // Hàng 2b: Chọn Nguồn dữ liệu
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Nguồn dữ liệu:", fontSize = 11.sp, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EDITOR_SOURCE_OPTIONS.forEach { (srcKey, srcLabel) ->
                    val isSelected = source.equals(srcKey, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) Color(0xFF1D4ED8) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF1E40AF) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.clickable {
                            source = srcKey
                            fieldState.source = srcKey
                        }
                    ) {
                        Text(
                            text = srcLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
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
