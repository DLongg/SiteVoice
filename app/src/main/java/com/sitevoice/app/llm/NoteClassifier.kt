package com.sitevoice.app.llm

import org.json.JSONObject

data class ClassificationResult(
    val id: Long,
    val label: String,
    val confidence: Double,
    val correctedText: String? = null
)

class NoteClassifier(
    private val llmClient: LlmClient
) {
    companion object {
        const val LABEL_THI_CONG = "thi_cong"
        const val LABEL_NGHIEM_THU = "nghiem_thu"
        const val LABEL_AN_TOAN = "an_toan"
        const val LABEL_Y_KIEN_CDT = "y_kien_cdt"
        const val LABEL_Y_KIEN_TVGS = "y_kien_tvgs"
        const val LABEL_Y_KIEN_NHA_THAU = "y_kien_nha_thau"
        const val LABEL_CHUA_PHAN_LOAI = "chua_phan_loai"

        val VALID_LABELS = setOf(
            LABEL_THI_CONG,
            LABEL_NGHIEM_THU,
            LABEL_AN_TOAN,
            LABEL_Y_KIEN_CDT,
            LABEL_Y_KIEN_TVGS,
            LABEL_Y_KIEN_NHA_THAU,
            LABEL_CHUA_PHAN_LOAI
        )

        private const val SYSTEM_PROMPT = """Bạn là trợ lý phân loại và hiệu đính ghi chú nhật ký công trường xây dựng Việt Nam.
Nhiệm vụ:
1. Phân loại MỖI ghi chú vào đúng một mục trong danh sách khóa.
2. Kiểm tra lỗi nhận diện âm thanh (STT) theo ngữ cảnh xây dựng Việt Nam và trả về văn bản đã hiệu đính trong trường "van_ban_da_sua".

Các khóa mục:
- "thi_cong": công việc đang/đã thi công, khối lượng, nhân lực làm việc, máy móc sử dụng
- "nghiem_thu": nghiệm thu, bàn giao, kiểm tra chất lượng hạng mục
- "an_toan": an toàn lao động, vệ sinh môi trường, sự cố, nhắc nhở an toàn
- "y_kien_cdt": ý kiến, yêu cầu, chỉ đạo của chủ đầu tư
- "y_kien_tvgs": ý kiến, yêu cầu, nhận xét của tư vấn giám sát
- "y_kien_nha_thau": ý kiến, kiến nghị của nhà thầu thi công
- "chua_phan_loai": không đủ thông tin để chắc chắn

Quy tắc:
- Tự động sửa các từ bị nhận diện sai âm (STT) dựa theo ngữ cảnh xây dựng Việt Nam vào trường "van_ban_da_sua".
- RÀNG BUỘC HIỆU ĐÍNH: Bắt buộc giữ nguyên 100% các số liệu, vị trí trục, tầng và ý nghĩa gốc của câu. Tuyệt đối không bịa đặt hay suy diễn thêm nội dung ngoài lời nói. Nếu câu không có lỗi STT, giữ nguyên văn bản gốc.
- Nếu ghi chú chứa nhiều ý gộp (ví dụ: vừa có thời tiết, vừa có thi công và nghiệm thu): ưu tiên chọn mục công việc cốt lõi (thi_cong, nghiem_thu hoặc an_toan) làm 'muc' đại diện.
- Nếu có từ khóa mở đầu ("Thi công:", "Nghiệm thu:", "An toàn:", "Chủ đầu tư:", "Giám sát:", "Nhà thầu:") thì theo từ khóa đó.
- Không chắc chắn thì chọn "chua_phan_loai", KHÔNG đoán.
- Trả về CHỈ JSON theo định dạng:
{"ket_qua": [{"id": 1, "muc": "thi_cong", "do_chac_chan": 0.95, "van_ban_da_sua": "văn bản đã hiệu đính"}]}"""
    }

    private fun quoteJson(str: String): String {
        val escaped = str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    suspend fun classify(id: Long, text: String): Result<ClassificationResult> {
        val userPrompt = """Dữ liệu vào:
[{"id": $id, "text": ${quoteJson(text)}}]"""

        val messages = listOf(
            LlmMessage(role = "system", content = SYSTEM_PROMPT),
            LlmMessage(role = "user", content = userPrompt)
        )

        val chatResult = llmClient.chat(messages, temperature = 0.0f, responseJson = true)
        if (chatResult.isFailure) {
            return Result.failure(chatResult.exceptionOrNull() ?: Exception("Lỗi gọi LLM"))
        }

        val rawContent = chatResult.getOrThrow()
        return try {
            val parsed = parseClassificationJson(rawContent, id)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseClassificationJson(jsonString: String, expectedId: Long): ClassificationResult {
        // Loại bỏ markdown fence nếu có
        var clean = jsonString.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        clean = clean.trim()

        // Trích xuất bằng Regex để đảm bảo hoạt động an toàn trên cả JVM Unit Test và Android Runtime
        val mucRegex = Regex("\"muc\"\\s*:\\s*\"([^\"]+)\"")
        val confRegex = Regex("\"do_chac_chan\"\\s*:\\s*([0-9.]+)")
        val idRegex = Regex("\"id\"\\s*:\\s*([0-9]+)")
        val correctedRegex = Regex("\"van_ban_da_sua\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")

        val mucMatch = mucRegex.find(clean)?.groupValues?.get(1)?.trim()
        val confMatch = confRegex.find(clean)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val idMatch = idRegex.find(clean)?.groupValues?.get(1)?.toLongOrNull() ?: expectedId
        val rawCorrected = correctedRegex.find(clean)?.groupValues?.get(1)
        val correctedMatch = rawCorrected?.replace("\\\"", "\"")?.replace("\\n", "\n")?.trim()?.ifBlank { null }

        val finalLabel = if (mucMatch != null && mucMatch in VALID_LABELS) {
            mucMatch
        } else {
            LABEL_CHUA_PHAN_LOAI
        }

        return ClassificationResult(
            id = idMatch,
            label = finalLabel,
            confidence = confMatch,
            correctedText = correctedMatch
        )
    }
}
