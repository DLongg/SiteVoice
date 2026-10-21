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
    private val validator: BudgetValidator = BudgetValidator(schema),
    private val customSystemPrompt: String? = null
) {
    companion object {
        private const val TAG = "ReportSynthesizer"

        const val DEFAULT_SYSTEM_PROMPT = """Bạn là thư ký công trường xây dựng chuyên nghiệp tại Việt Nam.
Từ các ghi chú được cung cấp trong ngày, hãy viết bản tổng hợp Nhật ký thi công hằng ngày bằng tiếng Việt, văn phong hành chính kỹ thuật ngắn gọn, súc tích để người dùng duyệt và xuất sang Google Doc chuẩn A4 tối đa 2 trang.

Quy tắc bắt buộc:
1. PHÂN BỔ ĐA MỤC TỪNG Ý:
   - Trong một bản ghi âm hoặc ghi chú, người nói có thể nói gộp nhiều nội dung thuộc nhiều mục khác nhau (ví dụ: một câu vừa có thời tiết, vừa có số lượng thợ, vừa có công việc thi công và công tác nghiệm thu).
   - Bạn PHẢI đọc kỹ từng câu, bóc tách từng ý trong mỗi ghi chú để phân bổ chính xác vào đúng mục tương ứng:
     * 'cong_viec_thi_cong': Toàn bộ các ý liên quan đến công tác triển khai thi công, vị trí (trục, tầng, phân đoạn), cấu kiện, khối lượng hoàn thành, máy móc sử dụng.
     * 'nghiem_thu': Toàn bộ các ý liên quan đến kiểm tra, đo đạc, nghiệm thu công việc, mẫu thí nghiệm, cốt thép, cốp pha...
     * 'an_toan': Toàn bộ các ý liên quan đến an toàn lao động, vệ sinh môi trường, trang bị bảo hộ, sự cố, nhắc nhở an toàn.
     * 'y_kien_cdt', 'y_kien_tvgs', 'y_kien_nha_thau': Toàn bộ ý kiến, yêu cầu hoặc chỉ đạo tương ứng của các bên.
     * 'chua_phan_loai': Các nội dung thuần túy không thuộc các mục kỹ thuật trên.
   - Tuyệt đối KHÔNG bỏ sót ý chỉ vì ghi chú đó được gán nhãn mục khác lúc ban đầu.
2. CHỈ dùng thông tin có trong ghi chú. Tuyệt đối KHÔNG tự bịa, KHÔNG suy đoán số liệu.
3. Giữ nguyên số liệu (khối lượng, kích thước), vị trí (trục, tầng, phân đoạn), tên hạng mục, tên đơn vị.
4. Mục nào không có dữ liệu: trả về mảng rỗng [] hoặc chuỗi rỗng "".
5. Gộp các ý trùng lặp, ưu tiên việc quan trọng (khối lượng hoàn thành, nghiệm thu, sự cố, an toàn, yêu cầu CĐT/TVGS).
6. Tuân thủ nghiêm ngặt giới hạn số dòng và số ký tự:
   - "cong_viec_thi_cong": mảng các dòng (tối đa 17 dòng, mỗi dòng ≤ 100 ký tự). Trình bày gom theo nhóm 3.1 [Tổ/Hạng mục 1], 3.2 [Tổ/Hạng mục 2]... Dưới mỗi nhóm là các gạch đầu dòng dạng "\t- [Nội dung và vị trí]".
     Ví dụ:
     [
       "3.1 Điện",
       "\t- Lắp đặt ống điện âm tường tầng 8.",
       "\t- Chôn đế, tủ điện tầng 8.",
       "3.2 Nước",
       "\t- Thi công trục thoát nước 10, 11 (lô gia).",
       "\t- Thăm ống, bể tự hoại tầng 1."
     ]
   - "nghiem_thu": mảng các dòng (tối đa 6 dòng, mỗi dòng ≤ 100 ký tự)
   - "an_toan": mảng các dòng (tối đa 3 dòng, mỗi dòng ≤ 100 ký tự)
   - "y_kien_cdt", "y_kien_tvgs", "y_kien_nha_thau": chuỗi văn bản (mỗi trường ≤ 200 ký tự)
   - "chua_phan_loai": mảng các dòng
7. Trả về CHỈ JSON theo đúng định dạng được yêu cầu, không kèm giải thích hay bọc markdown."""
    }

    suspend fun synthesizeReport(date: String, notes: List<NoteEntity>): Result<String> {
        val activeNotes = notes.filter { it.status != "DISCARDED" }.sortedBy { it.createdAt }

        // 1. Gom danh sách nguyên văn tất cả các ghi chú trong ngày
        val allNotesList = StringBuilder()
        activeNotes.forEachIndexed { index, note ->
            val text = note.text?.trim()
            if (!text.isNullOrBlank()) {
                val labelHint = note.label?.ifBlank { NoteClassifier.LABEL_CHUA_PHAN_LOAI } ?: NoteClassifier.LABEL_CHUA_PHAN_LOAI
                allNotesList.append("${index + 1}. [Gợi ý nhãn: $labelHint] $text\n")
            }
        }

        // 2. Gom theo danh mục tham khảo
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

        val dateParts = date.split("-")
        val year = dateParts.getOrNull(0) ?: ""
        val month = dateParts.getOrNull(1) ?: ""
        val day = dateParts.getOrNull(2) ?: ""

        val userPrompt = """Dữ liệu vào:
Ngày: $day - Tháng: $month - Năm: $year (Ngày đầy đủ: $date)

Danh sách toàn bộ các bản ghi âm/ghi chú nguyên văn trong ngày (lưu ý: mỗi ghi chú có thể chứa nhiều ý gộp):
$allNotesList

Phân nhóm sơ bộ tham khảo:
${groupedNotes.toString(2)}

Cấu trúc JSON mong muốn:
{
  "cong_viec_thi_cong": [
    "3.1 Điện",
    "\t- [Nội dung và vị trí]",
    "3.2 Nước",
    "\t- [Nội dung và vị trí]"
  ],
  "nghiem_thu": [],
  "an_toan": [],
  "y_kien_cdt": "",
  "y_kien_tvgs": "",
  "y_kien_nha_thau": "",
  "chua_phan_loai": []
}"""

        val effectiveSystemPrompt = customSystemPrompt?.ifBlank { DEFAULT_SYSTEM_PROMPT } ?: DEFAULT_SYSTEM_PROMPT
        val messages = listOf(
            LlmMessage(role = "system", content = effectiveSystemPrompt),
            LlmMessage(role = "user", content = userPrompt)
        )

        val chatResult = llmClient.chat(messages, temperature = 0.0f, responseJson = true)
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
