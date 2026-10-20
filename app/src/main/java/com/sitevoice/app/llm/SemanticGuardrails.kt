package com.sitevoice.app.llm

object SemanticGuardrails {

    const val MIN_ACCEPTABLE_CONFIDENCE = 0.70

    /**
     * Hậu kiểm tính nhất quán ngữ nghĩa và ràng buộc nhãn.
     */
    fun verify(text: String, candidateResult: ClassificationResult): ClassificationResult {
        val label = candidateResult.label.trim().lowercase()
        val lowerText = text.lowercase()

        // 1. Kiểm tra Enum hợp lệ
        if (!NoteClassifier.VALID_LABELS.contains(label)) {
            return candidateResult.copy(
                label = NoteClassifier.LABEL_CHUA_PHAN_LOAI,
                confidence = 0.5
            )
        }

        // 2. Ngưỡng tin cậy không đối xứng: gán sai tệ hơn để trống
        if (candidateResult.confidence < MIN_ACCEPTABLE_CONFIDENCE) {
            return candidateResult.copy(
                label = NoteClassifier.LABEL_CHUA_PHAN_LOAI,
                confidence = candidateResult.confidence
            )
        }

        // 3. Kiểm tra chéo (Contradiction Checks)
        when (label) {
            NoteClassifier.LABEL_Y_KIEN_CDT -> {
                val hasCdtSignal = lowerText.contains("chủ đầu tư") || lowerText.contains("cđt") || lowerText.contains("c d t")
                if (!hasCdtSignal) {
                    return candidateResult.copy(
                        label = NoteClassifier.LABEL_CHUA_PHAN_LOAI,
                        confidence = 0.5
                    )
                }
            }
            NoteClassifier.LABEL_Y_KIEN_TVGS -> {
                val hasTvgsSignal = lowerText.contains("tvgs") || lowerText.contains("tư vấn giám sát") ||
                        lowerText.contains("t v g s") || lowerText.contains("giám sát")
                if (!hasTvgsSignal) {
                    return candidateResult.copy(
                        label = NoteClassifier.LABEL_CHUA_PHAN_LOAI,
                        confidence = 0.5
                    )
                }
            }
            NoteClassifier.LABEL_Y_KIEN_NHA_THAU -> {
                val hasNhaThauSignal = lowerText.contains("nhà thầu") || lowerText.contains("kiến nghị") ||
                        lowerText.contains("đề xuất") || lowerText.contains("xin phê duyệt")
                if (!hasNhaThauSignal) {
                    return candidateResult.copy(
                        label = NoteClassifier.LABEL_CHUA_PHAN_LOAI,
                        confidence = 0.5
                    )
                }
            }
        }

        return candidateResult
    }
}
