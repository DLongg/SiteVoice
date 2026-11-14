package com.sitevoice.app.ui.template

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitevoice.app.R
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormLlmDescriber
import com.sitevoice.app.template.DefaultTemplateRepository
import com.sitevoice.app.template.TemplateSchema
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import androidx.compose.material3.MaterialTheme
import com.sitevoice.app.ui.theme.SiteVoiceTheme
import com.sitevoice.app.ui.theme.TextSizes
import com.sitevoice.app.ui.theme.Spacing

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
    isAutoCreating: Boolean = false,
    autoCreateError: String? = null,
    currentTemperature: Float = 0.2f,
    onTemperatureChange: ((Float) -> Unit)? = null,
    onAutoCreateOneShot: ((String, String?) -> Unit)? = null,
    onScanDocWithAi: ((String) -> Unit)? = null,
    onSyncFieldsFromDoc: ((String, (Result<List<FormCandidate>>) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(template?.name ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var docTemplateUrl by remember { mutableStateOf(template?.docTemplateUrl ?: "") }
    var temperature by remember(currentTemperature) {
        mutableFloatStateOf(currentTemperature.coerceIn(0.0f, 0.8f))
    }
    var showNewDocImporter by remember { mutableStateOf(template == null) }
    var sourceDocLinkInput by remember { mutableStateOf("") }
    var sourceCustomNameInput by remember { mutableStateOf("") }

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

    val fields = remember { mutableStateListOf<FieldEditorState>() }

    // Đồng bộ ngay lập tức khi tạo mẫu tự động 1 lần hoàn tất (hoặc khi mở bản mẫu duy nhất đang áp dụng)
    LaunchedEffect(template?.id, template?.updatedAt) {
        if (template != null) {
            name = template.name
            description = template.description
            docTemplateUrl = template.docTemplateUrl
            if (template.systemPrompt.isNotBlank()) {
                systemPrompt = template.systemPrompt
            }
            fields.clear()
            if (template.schemaJson.isNotBlank()) {
                try {
                    val rawJson = JSONObject(template.schemaJson)
                    val summaryMap = rawJson.optJSONObject("truong_tong_hop")
                        ?: rawJson.optJSONObject("summary_fields")
                    val schema = TemplateSchema.fromJson(template.schemaJson)
                    schema.summaryFields.forEach { (key, config) ->
                        val rawFieldObj = summaryMap?.optJSONObject(key)
                        val label = rawFieldObj?.optString("label")?.ifBlank { key } ?: key
                        val src = config.sourceCategories.firstOrNull() ?: "thi_cong"
                        fields.add(
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
            showNewDocImporter = false
        } else {
            showNewDocImporter = true
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
            templateName = name.ifBlank { "Nhật ký tự động" }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.lg)
    ) {
        // TopBar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.btn_close),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.xs))
                Column {
                    Text(
                        text = if (template == null) "Thiết lập Nhật ký tự động" else "Nhật ký tự động & Chỉnh sửa Mẫu",
                        fontSize = TextSizes.title,
                        lineHeight = TextSizes.titleLine,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (template == null)
                            "Dán link Google Doc để tạo bản mẫu tự động một lần"
                        else
                            "Một bản mẫu duy nhất • Sửa Form, các thẻ {{MA}} và System Prompt",
                        fontSize = TextSizes.caption,
                        lineHeight = TextSizes.captionLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Nội dung cuộn
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Khối Tạo Mẫu Tự Động 1 Lần từ Link Google Doc (hiện mặc định khi chưa có mẫu hoặc khi bấm Đổi file Doc gốc)
            if (template == null || showNewDocImporter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.medium)
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tạo Mẫu Tự Động Từ Google Doc",
                                fontSize = TextSizes.subtitle,
                                lineHeight = TextSizes.subtitleLine,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (template != null) {
                            TextButton(
                                onClick = { showNewDocImporter = false },
                                contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Text("Đóng", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Lưu ý minh bạch: Ứng dụng sẽ tạo 1 bản sao trên Google Drive của bạn (tuyệt đối không sửa file gốc), quét các chỗ điền (...) và gửi danh sách vị trí cho nhà cung cấp AI (LLM) để đề xuất thẻ {{MA}} & System Prompt. Bạn sẽ duyệt qua 2 bước trước khi ghi vào bản sao Google Doc.",
                        fontSize = TextSizes.caption,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = TextSizes.bodyLine
                    )

                    OutlinedTextField(
                        value = sourceDocLinkInput,
                        onValueChange = {
                            sourceDocLinkInput = it
                            errorMessage = null
                        },
                        label = { Text("Link hoặc ID Google Doc biểu mẫu gốc") },
                        placeholder = { Text("https://docs.google.com/document/d/...", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                        singleLine = true,
                        enabled = !isAutoCreating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium),
                        shape = MaterialTheme.shapes.medium
                    )

                    OutlinedTextField(
                        value = sourceCustomNameInput,
                        onValueChange = { sourceCustomNameInput = it },
                        label = { Text("Tên bản mẫu (để trống sẽ tự lấy theo tên Doc)") },
                        placeholder = { Text("Ví dụ: Nhật ký tự động công trình A", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                        singleLine = true,
                        enabled = !isAutoCreating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium),
                        shape = MaterialTheme.shapes.medium
                    )

                    autoCreateError?.let { err ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = err,
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (sourceDocLinkInput.isBlank()) {
                                errorMessage = "Vui lòng dán Link hoặc ID Google Doc trước khi tạo mẫu tự động."
                            } else if (onAutoCreateOneShot != null) {
                                errorMessage = null
                                onAutoCreateOneShot(
                                    sourceDocLinkInput.trim(),
                                    sourceCustomNameInput.trim().takeIf { it.isNotBlank() }
                                )
                            } else if (onScanDocWithAi != null) {
                                errorMessage = null
                                onScanDocWithAi(sourceDocLinkInput.trim())
                            }
                        },
                        enabled = !isAutoCreating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isAutoCreating) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = "Đang tạo bản sao & phân tích chỗ điền bằng AI...",
                                fontSize = TextSizes.body,
                                lineHeight = TextSizes.bodyLine,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Phân Tích & Duyệt Mẫu Tự Động (2 Bước)",
                                    fontSize = TextSizes.body,
                                    lineHeight = TextSizes.bodyLine,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Khi đã có Bản Mẫu Duy Nhất -> Hiển thị đầy đủ các mục để người dùng chỉnh sửa
            if (template != null) {
                // Banner Bản Mẫu Duy Nhất đang áp dụng + nút nạp file Doc gốc khác nếu cần
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SiteVoiceTheme.colors.successContainer, MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                        .padding(horizontal = Spacing.md, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = SiteVoiceTheme.colors.onSuccessContainer
                            )
                            Text(
                                text = "Đang áp dụng Bản Mẫu Duy Nhất",
                                fontSize = TextSizes.body,
                                lineHeight = TextSizes.bodyLine,
                                fontWeight = FontWeight.Bold,
                                color = SiteVoiceTheme.colors.onSuccessContainer
                            )
                        }
                        Text(
                            text = "Chỉnh sửa Form, các thẻ {{MA}} và System Prompt ngay bên dưới",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            color = SiteVoiceTheme.colors.success
                        )
                    }
                    if (!showNewDocImporter) {
                        OutlinedButton(
                            onClick = { showNewDocImporter = true },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                        ) {
                            Text(
                                text = "Đổi Doc gốc",
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                fontWeight = FontWeight.SemiBold,
                                color = SiteVoiceTheme.colors.onSuccessContainer
                            )
                        }
                    }
                }

                // Phần 1: Form & Bản sao Google Doc
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.medium)
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        text = "1. Form & Bản sao Google Doc",
                        fontSize = TextSizes.subtitle,
                        lineHeight = TextSizes.subtitleLine,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                        label = { Text("Link Bản Sao Google Doc (đã chèn thẻ {{MA}})") },
                        placeholder = { Text(stringResource(id = R.string.template_field_doc_url_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Khối thao tác sửa vị trí trực tiếp bằng tay trên Google Doc & Đồng bộ ngược
                    if (docTemplateUrl.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = MaterialTheme.shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sửa Form & vị trí thẻ {{MA}} trực tiếp trên Google Doc:",
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Mở bản sao Google Doc để xem hoặc tự tay dời/gõ các thẻ {{MA}} (VD: {{MAY_MOC}}, {{Y_KIEN_CDT}}), sau đó bấm Đồng bộ lại.",
                                    fontSize = TextSizes.caption,
                                    lineHeight = TextSizes.captionLine,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
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
                                        shape = MaterialTheme.shapes.small,
                                        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.sm)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.template_editor_open_doc_btn),
                                            fontSize = TextSizes.caption,
                                            lineHeight = TextSizes.captionLine,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                                                        syncStatusMessage = "✓ Đã đồng bộ ${fields.size} thẻ từ Google Doc!"
                                                    } else {
                                                        errorMessage = result.exceptionOrNull()?.message ?: "Lỗi đồng bộ từ Google Doc"
                                                    }
                                                }
                                            },
                                            enabled = !isSyncingDoc,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = MaterialTheme.shapes.small,
                                            contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.sm)
                                        ) {
                                            if (isSyncingDoc) {
                                                CircularProgressIndicator(
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(Spacing.sm))
                                            }
                                            Text(
                                                text = stringResource(id = R.string.template_editor_rescan_doc_btn),
                                                fontSize = TextSizes.caption,
                                                lineHeight = TextSizes.captionLine,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }

                                syncStatusMessage?.let { msg ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = SiteVoiceTheme.colors.success
                                        )
                                        Text(
                                            text = msg,
                                            fontSize = TextSizes.caption,
                                            lineHeight = TextSizes.captionLine,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SiteVoiceTheme.colors.success
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Phần 2: Các mục & Các thẻ {{MA}} trong mẫu
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.medium)
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Các mục & Các thẻ {{MA}} trong mẫu",
                            fontSize = TextSizes.subtitle,
                            lineHeight = TextSizes.subtitleLine,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${fields.size} thẻ",
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Chỉnh sửa mã thẻ {{MA}}, tên mục, kiểu dữ liệu, nguồn ghi chú và vị trí neo trong Google Doc.",
                        fontSize = TextSizes.caption,
                        lineHeight = TextSizes.captionLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (fields.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = MaterialTheme.shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(id = R.string.template_editor_empty_fields_title),
                                    fontSize = TextSizes.body,
                                    lineHeight = TextSizes.bodyLine,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = stringResource(id = R.string.template_editor_empty_fields_desc),
                                    fontSize = TextSizes.caption,
                                    lineHeight = TextSizes.captionLine,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(text = stringResource(id = R.string.template_btn_add_field), fontSize = TextSizes.body, lineHeight = TextSizes.bodyLine)
                    }
                }

                // Phần 3: System Prompt & Nhiệt độ AI
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.medium)
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. System Prompt & Nhiệt độ AI",
                            fontSize = TextSizes.subtitle,
                            lineHeight = TextSizes.subtitleLine,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (fields.isEmpty()) {
                                    errorMessage = "Cần ít nhất 1 thẻ ở mục 2 để Render System Prompt."
                                } else {
                                    errorMessage = null
                                    systemPrompt = renderPromptFromCurrentFields()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm)
                        ) {
                            Text(
                                text = stringResource(id = R.string.template_editor_render_prompt_btn),
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Thanh trượt Nhiệt độ AI ngay trong màn hình sửa bản mẫu
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nhiệt độ tổng hợp (Temperature):",
                                    fontSize = TextSizes.caption,
                                    lineHeight = TextSizes.captionLine,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.2f", temperature),
                                        fontSize = TextSizes.caption,
                                        lineHeight = TextSizes.captionLine,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Slider(
                                value = temperature,
                                onValueChange = {
                                    temperature = it.coerceIn(0.0f, 0.8f)
                                    onTemperatureChange?.invoke(temperature)
                                },
                                valueRange = 0.0f..0.8f
                            )
                            Text(
                                text = "0.00 = bám sát nguyên văn • 0.20 = mượt mà, không bịa số liệu • 0.80 = diễn đạt linh hoạt",
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Bạn có thể sửa trực tiếp System Prompt bên dưới hoặc bấm '🔄 Render Prompt từ các trường tổng hợp' để cập nhật theo danh sách thẻ ở mục 2.",
                        fontSize = TextSizes.caption,
                        lineHeight = TextSizes.captionLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 7,
                        maxLines = 16
                    )
                }
            }

            // Báo lỗi nếu có
            errorMessage?.let { msg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = TextSizes.body,
                        lineHeight = TextSizes.bodyLine,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Nút Lưu Bản Mẫu Duy Nhất (chỉ hiển thị khi đã tạo mẫu)
        if (template != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Vui lòng nhập tên bản mẫu"
                        return@Button
                    }
                    if (fields.isEmpty()) {
                        errorMessage = "Cần ít nhất một thẻ placeholder trong mẫu"
                        return@Button
                    }

                    val schemaObj = JSONObject()
                    val id = DefaultTemplateRepository.SINGLE_AUTO_TEMPLATE_ID
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
                        isBuiltIn = false,
                        isActive = true,
                        createdAt = template.createdAt,
                        updatedAt = System.currentTimeMillis()
                    )

                    onTemperatureChange?.invoke(temperature)
                    onSaveTemplate(updated)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(id = R.string.template_btn_save_template),
                    fontWeight = FontWeight.Bold,
                    fontSize = TextSizes.subtitle,
                    lineHeight = TextSizes.subtitleLine
                )
            }
        }
    }
}

@Composable
fun FieldItemEditor(
    fieldState: FieldEditorState,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    var key by remember(fieldState) { mutableStateOf(fieldState.key) }
    var label by remember(fieldState) { mutableStateOf(fieldState.label) }
    var type by remember(fieldState) { mutableStateOf(fieldState.type) }
    var source by remember(fieldState) { mutableStateOf(fieldState.source) }
    var maxLines by remember(fieldState) { mutableIntStateOf(fieldState.maxLines) }
    var maxCharsPerLine by remember(fieldState) { mutableIntStateOf(fieldState.maxCharsPerLine) }
    var tieuDeGanNhat by remember(fieldState) { mutableStateOf(fieldState.tieuDeGanNhat) }
    var truoc by remember(fieldState) { mutableStateOf(fieldState.truoc) }
    var sau by remember(fieldState) { mutableStateOf(fieldState.sau) }
    var startIndex by remember(fieldState) { mutableIntStateOf(fieldState.startIndex) }
    var endIndex by remember(fieldState) { mutableIntStateOf(fieldState.endIndex) }
    var showPositionEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.medium)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Hàng 1: Mã placeholder + Tên mục + Nút xóa
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                    fieldState.key = it
                },
                label = { Text("Mã thẻ {{...}}", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = label,
                onValueChange = {
                    label = it
                    fieldState.label = it
                },
                label = { Text("Tên mục hiển thị", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                modifier = Modifier.weight(1.2f),
                singleLine = true
            )

            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.notes_delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Khung tóm tắt & nút mở sửa Vị trí trong bản mẫu Google Doc
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPositionEditor = !showPositionEditor }
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.field_position_section_title),
                        fontSize = TextSizes.caption,
                        lineHeight = TextSizes.captionLine,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { showPositionEditor = !showPositionEditor },
                        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Text(
                                text = if (showPositionEditor) "Thu gọn" else "Sửa vị trí",
                                fontSize = TextSizes.caption,
                                lineHeight = TextSizes.captionLine,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (showPositionEditor) Icons.Default.KeyboardArrowUp else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                    fontSize = TextSizes.caption,
                    lineHeight = TextSizes.captionLine,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showPositionEditor) {
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value = tieuDeGanNhat,
                        onValueChange = {
                            tieuDeGanNhat = it
                            fieldState.tieuDeGanNhat = it
                        },
                        label = { Text(stringResource(id = R.string.field_position_heading_label), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = truoc,
                            onValueChange = {
                                truoc = it
                                fieldState.truoc = it
                            },
                            label = { Text(stringResource(id = R.string.field_position_before_label), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = sau,
                            onValueChange = {
                                sau = it
                                fieldState.sau = it
                            },
                            label = { Text(stringResource(id = R.string.field_position_after_label), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = if (startIndex >= 0) startIndex.toString() else "",
                            onValueChange = { str ->
                                val v = str.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: -1
                                startIndex = v
                                fieldState.startIndex = v
                            },
                            label = { Text(stringResource(id = R.string.field_position_start_index), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
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
                            label = { Text(stringResource(id = R.string.field_position_end_index), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Hàng 2: Chọn kiểu dữ liệu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Kiểu:", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FilterChip(
                selected = type == "danh_sach",
                onClick = {
                    type = "danh_sach"
                    fieldState.type = "danh_sach"
                },
                label = { Text(stringResource(id = R.string.template_field_type_list), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            FilterChip(
                selected = type == "van_ban",
                onClick = {
                    type = "van_ban"
                    fieldState.type = "van_ban"
                },
                label = { Text(stringResource(id = R.string.template_field_type_text), fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        // Hàng 2b: Chọn Nguồn dữ liệu
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Nguồn dữ liệu:", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                EDITOR_SOURCE_OPTIONS.forEach { (srcKey, srcLabel) ->
                    val isSelected = source.equals(srcKey, ignoreCase = true)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.clickable {
                            source = srcKey
                            fieldState.source = srcKey
                        }
                    ) {
                        Text(
                            text = srcLabel,
                            fontSize = TextSizes.caption,
                            lineHeight = TextSizes.captionLine,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Dòng:", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(Spacing.xs))
                IconButton(
                    onClick = {
                        if (maxLines > 1) {
                            maxLines--
                            fieldState.maxLines = maxLines
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Giảm dòng",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$maxLines",
                    fontSize = TextSizes.body,
                    lineHeight = TextSizes.bodyLine,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Spacing.xs)
                )
                IconButton(
                    onClick = {
                        maxLines++
                        fieldState.maxLines = maxLines
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tăng dòng",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Ký tự/dòng:", fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(Spacing.xs))
                IconButton(
                    onClick = {
                        if (maxCharsPerLine > 30) {
                            maxCharsPerLine -= 10
                            fieldState.maxCharsPerLine = maxCharsPerLine
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Giảm ký tự",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$maxCharsPerLine",
                    fontSize = TextSizes.body,
                    lineHeight = TextSizes.bodyLine,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Spacing.xs)
                )
                IconButton(
                    onClick = {
                        maxCharsPerLine += 10
                        fieldState.maxCharsPerLine = maxCharsPerLine
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tăng ký tự",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
