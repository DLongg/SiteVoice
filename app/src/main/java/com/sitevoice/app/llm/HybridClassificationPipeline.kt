package com.sitevoice.app.llm

class HybridClassificationPipeline(
    private val llmClassifier: NoteClassifier? = null
) {
    /**
     * Phân loại văn bản qua đường ống đa tầng:
     * Tầng 1 (User) -> Tiền xử lý (Normalizer, Splitter) -> Tầng 2 & 3 (Rule Engine) -> Tầng 4 (Context) -> Tầng 5 (LLM + Guardrails).
     */
    suspend fun classify(
        id: Long,
        rawText: String,
        userLabel: String? = null,
        previousNoteLabel: String? = null
    ): ClassificationResult {
        // TẦNG 1: Nhãn do người dùng đã sửa (Ưu tiên tuyệt đối)
        if (!userLabel.isNullOrBlank() && NoteClassifier.VALID_LABELS.contains(userLabel)) {
            return ClassificationResult(id, userLabel, 1.0)
        }

        // BƯỚC 1: Chuẩn hóa từ điển & sửa lỗi nghe
        val normalized = TextNormalizer.normalize(rawText)

        // BƯỚC 2: Tách mệnh đề
        val clauses = ClauseSplitter.splitIntoClauses(normalized)

        // TẦNG 2 & 3: Tầng quy tắc tất định 0ms Offline
        val ruleResult = DeterministicRuleEngine.evaluate(normalized, clauses)
        if (ruleResult != null && ruleResult.confidence >= 0.90) {
            return ClassificationResult(id, ruleResult.label, ruleResult.confidence)
        }

        // TẦNG 4: Kế thừa ngữ cảnh cho câu ngắn phụ thuộc
        val words = normalized.trim().split("\\s+".toRegex())
        val lower = normalized.lowercase()

        // Trường hợp nghiệm thu đạt ngắn gọn: "đạt rồi", "cho nghiệm thu"
        if (lower.contains("đạt rồi") || lower.contains("cho chuyển bước") || lower.contains("cho chuyển tiếp")) {
            return ClassificationResult(id, NoteClassifier.LABEL_NGHIEM_THU, 0.90)
        }

        // Nếu câu ngắn và có câu trước: kế thừa mục câu trước
        if (words.size <= 6 && !previousNoteLabel.isNullOrBlank() && NoteClassifier.VALID_LABELS.contains(previousNoteLabel)) {
            return ClassificationResult(id, previousNoteLabel, 0.85)
        }

        // TẦNG 5: Gọi LLM (nếu có LLM client) và Hậu kiểm Guardrails
        if (llmClassifier != null) {
            val llmRes = llmClassifier.classify(id, normalized)
            if (llmRes.isSuccess) {
                val candidate = llmRes.getOrThrow()
                return SemanticGuardrails.verify(normalized, candidate)
            }
        }

        // Fallback an toàn cuối cùng
        return ClassificationResult(id, NoteClassifier.LABEL_CHUA_PHAN_LOAI, 0.5)
    }
}
