package com.sitevoice.app.llm

import android.util.Log
import com.sitevoice.app.data.NoteEntity
import com.sitevoice.app.template.BudgetValidator
import com.sitevoice.app.template.SummaryFieldConfig
import com.sitevoice.app.template.TemplateSchema
import org.json.JSONArray
import org.json.JSONObject

class ReportSynthesizer(
    private val llmClient: LlmClient,
    private val schema: TemplateSchema,
    private val validator: BudgetValidator = BudgetValidator(schema)
) {
    companion object {
        private const val TAG = "ReportSynthesizer"

        private const val SYSTEM_PROMPT = """Bạn là thư ký công trường xây dựng chuyên nghiệp tại Việt Nam.
Từ các ghi chú được cung cấp trong ngày, hãy viết bản tổng hợp Nhật ký thi công hằng ngày bằng tiếng Việt, văn phong hành chính kỹ thuật ngắn gọn, súc tích để người dùng duyệt.

Quy tắc bắt buộc:
1. CHỈ dùng thông tin có trong ghi chú. Tuyệt đối KHÔNG bịa, KHÔNG suy đoán số liệu.
2. Giữ nguyên số liệu (khối lượng, kích thước), vị trí (trục, tầng, phân đoạn), tên hạng mục, tên đơn vị.
3. Mục nào không có dữ liệu: trả về mảng rỗng [] hoặc chuỗi rỗng "".
4. Gộp các ý trùng lặp, ưu tiên việc quan trọng (khối lượng hoàn thành, nghiệm thu, sự cố, an toàn, yêu cầu CĐT/TVGS).
5. Ghi chú thuộc "chua_phan_loai": KHÔNG tự gán vào mục khác, đưa vào trường "chua_phan_loai".
6. Tuân thủ nghiêm ngặt giới hạn số dòng và số ký tự:
   - "cong_viec_thi_cong": mảng các dòng (tối đa 15 dòng, mỗi dòng ≤ 100 ký tự)
   - "nghiem_thu": mảng các dòng (tối đa 6 dòng, mỗi dòng ≤ 100 ký tự)
   - "an_toan": mảng các dòng (tối đa 3 dòng, mỗi dòng ≤ 100 ký tự)
   - "y_kien_cdt", "y_kien_tvgs", "y_kien_nha_thau": chuỗi văn bản (mỗi trường ≤ 200 ký tự)
   - "chua_phan_loai": mảng các dòng
7. Trả về CHỈ JSON theo đúng định dạng được yêu cầu, không kèm giải thích hay bọc markdown."""
    }

    suspend fun synthesizeReport(date: String, notes: List<NoteEntity>): Result<String> {
        val activeNotes = notes.filter { it.status != "DISCARDED" }

        // Gom các ghi chú theo mục
        val groupedNotes = JSONObject()
        val categories = listOf(
            NoteClassifier.LABEL_THI_CONG,
            NoteClassifier.LABEL_NGHIEM_THU,
            NoteClassifier.LABEL_AN_TOAN,
            NoteClassifier.LABEL_Y_KIEN_CDT,
            NoteClassifier.LABEL_Y_KIEN_TVGS,
            NoteClassifier.LABEL_Y_KIEN_NHA_THAU,
            NoteClassifier.LABEL_CHUA_PHAN_LOAI
        )

        for (cat in categories) {
            val list = JSONArray()
            val catNotes = activeNotes.filter {
                val label = it.label ?: NoteClassifier.LABEL_CHUA_PHAN_LOAI
                if (cat == NoteClassifier.LABEL_CHUA_PHAN_LOAI) {
                    label == NoteClassifier.LABEL_CHUA_PHAN_LOAI || label == "unclassified" || label.isBlank()
                } else {
                    label == cat
                }
            }
            for (n in catNotes) {
                val text = n.text
                if (!text.isNullOrBlank()) {
                    list.put(text)
                }
            }
            groupedNotes.put(cat, list)
        }

        val userPrompt = """Dữ liệu vào:
Ngày báo cáo: $date
Danh sách ghi chú theo từng mục:
${groupedNotes.toString(2)}

Yêu cầu định dạng JSON trả về:
{
  "cong_viec_thi_cong": ["..."],
  "nghiem_thu": ["..."],
  "an_toan": ["..."],
  "y_kien_cdt": "...",
  "y_kien_tvgs": "...",
  "y_kien_nha_thau": "...",
  "chua_phan_loai": ["..."]
}"""

        val messages = listOf(
            LlmMessage(role = "system", content = SYSTEM_PROMPT),
            LlmMessage(role = "user", content = userPrompt)
        )

        val chatResult = llmClient.chat(messages, temperature = 0.1f, responseJson = true)
        if (chatResult.isFailure) {
            return Result.failure(chatResult.exceptionOrNull() ?: Exception("Lỗi gọi LLM tổng hợp"))
        }

        var reportJson = cleanJson(chatResult.getOrThrow())

        // Vòng kiểm tra ngân sách
        val validationResult = validator.validate(reportJson)
        if (!validationResult.isValid) {
            Log.w(TAG, "Bản tổng hợp ban đầu vượt ngân sách: ${validationResult.violations}")
            // Thử rút gọn các trường vượt ngân sách
            reportJson = tryShrinkViolations(reportJson, validationResult.violations)
        }

        return Result.success(reportJson)
    }

    private suspend fun tryShrinkViolations(
        originalJson: String,
        violations: List<com.sitevoice.app.template.BudgetViolation>
    ): String {
        try {
            val jsonObj = JSONObject(originalJson)
            val fieldsToShrink = violations.map { it.fieldName.substringBefore("[") }.distinct()

            for (fieldName in fieldsToShrink) {
                val config = schema.summaryFields[fieldName] ?: continue
                val currentVal = jsonObj.opt(fieldName) ?: continue

                val shrinkResult = shrinkSingleField(fieldName, currentVal, config)
                if (shrinkResult.isSuccess) {
                    val newVal = shrinkResult.getOrThrow()
                    jsonObj.put(fieldName, newVal)
                }
            }

            return jsonObj.toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong vòng rút gọn: ${e.message}", e)
            return originalJson
        }
    }

    private suspend fun shrinkSingleField(
        fieldName: String,
        currentValue: Any,
        config: SummaryFieldConfig
    ): Result<Any> {
        val limitDesc = if (config.type == "danh_sach") {
            "tối đa ${config.maxLines} dòng, mỗi dòng ≤ ${config.maxCharsPerLine} ký tự"
        } else {
            "tổng độ dài ≤ ${config.maxChars} ký tự"
        }

        val systemPrompt = """Bạn là trợ lý biên tập kỹ thuật công trường.
Nhiệm vụ: Rút gọn nội dung dưới đây để tuân thủ giới hạn ngân sách nghiêm ngặt:
- Giữ nguyên số liệu, vị trí, tên hạng mục quan trọng.
- Gộp các ý trùng, bỏ từ đệm và chi tiết phụ.
- KHÔNG thêm thông tin mới.
- Trả về CHỈ JSON theo định dạng: {"noi_dung": ...} (trong đó noi_dung có cùng kiểu dữ liệu với nội dung ban đầu)."""

        val userPrompt = """- Tên trường: $fieldName
- Giới hạn: $limitDesc
- Nội dung hiện tại:
$currentValue"""

        val messages = listOf(
            LlmMessage(role = "system", content = systemPrompt),
            LlmMessage(role = "user", content = userPrompt)
        )

        val res = llmClient.chat(messages, temperature = 0.0f, responseJson = true)
        if (res.isFailure) return Result.failure(res.exceptionOrNull() ?: Exception("Lỗi rút gọn"))

        return try {
            val clean = cleanJson(res.getOrThrow())
            val resObj = JSONObject(clean)
            if (resObj.has("noi_dung")) {
                Result.success(resObj.get("noi_dung"))
            } else {
                Result.success(currentValue)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) clean = clean.removePrefix("```json")
        if (clean.startsWith("```")) clean = clean.removePrefix("```")
        if (clean.endsWith("```")) clean = clean.removeSuffix("```")
        return clean.trim()
    }
}
