package com.sitevoice.app.llm

data class RuleMatchResult(
    val label: String,
    val confidence: Double,
    val matchedRule: String
)

object DeterministicRuleEngine {

    /**
     * Đánh giá văn bản bằng bộ quy tắc tất định.
     * Trả về RuleMatchResult nếu độ tin cậy >= 0.90, ngược lại trả về null để chuyển cho LLM.
     */
    fun evaluate(cleanText: String, clauses: List<String> = emptyList()): RuleMatchResult? {
        val lower = cleanText.lowercase().trim()
        val allClauses = if (clauses.isNotEmpty()) clauses else listOf(cleanText)
        val firstClause = allClauses.firstOrNull()?.lowercase() ?: lower

        // TẦNG 2: Cue words mở đầu (Độ tin cậy 1.0)
        if (lower.startsWith("thi công:") || lower.startsWith("thi công :") || lower.startsWith("thi công ")) {
            return RuleMatchResult("thi_cong", 1.0, "cue_thi_cong")
        }
        if (lower.startsWith("nghiệm thu:") || lower.startsWith("nghiệm thu :") || lower.startsWith("nghiệm thu ")) {
            return RuleMatchResult("nghiem_thu", 1.0, "cue_nghiem_thu")
        }
        if (lower.startsWith("an toàn:") || lower.startsWith("an toàn :")) {
            return RuleMatchResult("an_toan", 1.0, "cue_an_toan")
        }
        if (lower.startsWith("chủ đầu tư:") || lower.startsWith("cđt:")) {
            return RuleMatchResult("y_kien_cdt", 1.0, "cue_y_kien_cdt")
        }
        if (lower.startsWith("tư vấn giám sát:") || lower.startsWith("tvgs:") || lower.startsWith("giám sát:")) {
            return RuleMatchResult("y_kien_tvgs", 1.0, "cue_y_kien_tvgs")
        }
        if (lower.startsWith("nhà thầu:") || lower.startsWith("nhà thầu kiến nghị") || lower.startsWith("nhà thầu đề nghị") || lower.startsWith("nhà thầu đề xuất")) {
            return RuleMatchResult("y_kien_nha_thau", 1.0, "cue_y_kien_nha_thau")
        }

        // TẦNG 3: Strong Lexicon & Clause Analysis

        // 3.1. Nhà thầu phát ngôn (nếu mệnh đề đầu là chủ ngữ Nhà thầu)
        if (firstClause.contains("nhà thầu") && (firstClause.contains("kiến nghị") || firstClause.contains("đề nghị") || firstClause.contains("đề xuất") || firstClause.contains("xin phê duyệt"))) {
            return RuleMatchResult("y_kien_nha_thau", 0.98, "lexicon_nha_thau_subject")
        }

        // 3.2. Biên bản nghiệm thu / Nghiệm thu công việc (ưu tiên kết quả nghiệm thu)
        val isNghiemThuAction = lower.contains("lập biên bản nghiệm thu") ||
                lower.contains("đồng ý nghiệm thu") ||
                lower.contains("ký biên bản nghiệm thu") ||
                lower.contains("chấp thuận nghiệm thu") ||
                lower.contains("đạt yêu cầu chuyển giai đoạn") ||
                lower.contains("nghiệm thu áp lực") ||
                lower.contains("bàn giao mặt bằng") ||
                (lower.contains("nghiệm thu ván khuôn") || lower.contains("nghiệm thu cốt thép") || lower.contains("nghiệm thu công việc"))

        if (isNghiemThuAction) {
            return RuleMatchResult("nghiem_thu", 0.98, "lexicon_nghiem_thu_action")
        }

        // 3.3. Ý kiến TVGS (TVGS / Giám sát + Động từ chỉ đạo)
        for (clause in allClauses) {
            val cLower = clause.lowercase()
            if (cLower.contains("tvgs") || cLower.contains("tư vấn giám sát") || cLower.contains("giám sát")) {
                if (cLower.contains("yêu cầu") || cLower.contains("nhắc nhở") || cLower.contains("đề nghị") ||
                    cLower.contains("nhận xét") || cLower.contains("đình chỉ")
                ) {
                    return RuleMatchResult("y_kien_tvgs", 0.98, "lexicon_tvgs_action")
                }
            }
        }

        // 3.4. Ý kiến Chủ đầu tư (CĐT + Động từ chỉ đạo)
        for (clause in allClauses) {
            val cLower = clause.lowercase()
            if (cLower.contains("chủ đầu tư") || cLower.contains("cđt")) {
                if (cLower.contains("yêu cầu") || cLower.contains("nhắc nhở") || cLower.contains("chỉ đạo") ||
                    cLower.contains("đồng ý phương án") || cLower.contains("nhắc tiến độ") || cLower.contains("nhắc nhở về") ||
                    cLower.contains("dừng máy")
                ) {
                    return RuleMatchResult("y_kien_cdt", 0.98, "lexicon_cdt_action")
                }
            }
        }

        // 3.5. Nhà thầu đề xuất/kiến nghị chung
        for (clause in allClauses) {
            val cLower = clause.lowercase()
            if (cLower.contains("nhà thầu")) {
                if (cLower.contains("kiến nghị") || cLower.contains("đề nghị") || cLower.contains("đề xuất") ||
                    cLower.contains("xin phê duyệt") || cLower.contains("báo cáo tiến độ")
                ) {
                    return RuleMatchResult("y_kien_nha_thau", 0.95, "lexicon_nha_thau_action")
                }
            }
        }

        // 3.6. An toàn lao động & Vệ sinh môi trường
        val isAnToan = lower.contains("dây an toàn") || lower.contains("mũ bảo hộ") || lower.contains("giày mũi sắt") ||
                lower.contains("kính hàn") || lower.contains("khẩu trang") || lower.contains("lan can") ||
                lower.contains("rào chắn") || lower.contains("sự cố") || lower.contains("tai nạn") ||
                lower.contains("vệ sinh môi trường") || lower.contains("bình chữa cháy") ||
                (lower.contains("không đeo") && lower.contains("an toàn")) ||
                (lower.contains("chưa an toàn") && lower.contains("rào chắn")) ||
                lower.contains("kế hoạch an toàn") || lower.contains("đình chỉ công việc") ||
                lower.contains("đảm bảo an toàn")

        if (isAnToan) {
            return RuleMatchResult("an_toan", 0.95, "lexicon_an_toan")
        }

        // 3.7. Nghiệm thu & Kiểm tra chất lượng chung
        val isNghiemThuGeneral = lower.contains("nghiệm thu") ||
                lower.contains("biên bản kiểm tra") || lower.contains("tổ mẫu") ||
                lower.contains("tim trục móng") ||
                lower.contains("đúng sai số cho phép")

        if (isNghiemThuGeneral) {
            return RuleMatchResult("nghiem_thu", 0.95, "lexicon_nghiem_thu_general")
        }

        // 3.8. Thi công xây lắp (kể cả có nhắc độ sụt khi nhận bê tông để đổ)
        val isThiCong = lower.contains("đổ bê tông") || lower.contains("đổ xong") || lower.contains("bơm lên mái") ||
                lower.contains("cốt thép") || lower.contains("gia công") || lower.contains("lắp dựng") ||
                lower.contains("xây tường") || lower.contains("trát tường") || lower.contains("cẩu tháp") ||
                lower.contains("bảo dưỡng ẩm") || lower.contains("hàn nối") || lower.contains("hệ giáo") ||
                lower.contains("ván khuôn") || lower.contains("máy đục") || lower.contains("đục phá dỡ") ||
                lower.contains("đục xong tường") || lower.contains("đục đầu cọc") ||
                lower.contains("bao xi măng") || lower.contains("khối cát") || lower.contains("mác 300") ||
                lower.contains("đội thợ") || lower.contains("làm việc tăng ca") || lower.contains("lắp đặt") ||
                lower.contains("ống luồn dây") || (lower.contains("bê tông về") && lower.contains("bơm"))

        if (isThiCong) {
            return RuleMatchResult("thi_cong", 0.92, "lexicon_thi_cong")
        }

        // 3.9. Mơ hồ / Chưa phân loại rõ ràng
        if (lower.startsWith("thời tiết hôm nay") || lower.startsWith("alo 1 2 3") || lower.startsWith("test mic")) {
            return RuleMatchResult("chua_phan_loai", 1.0, "rule_ambiguous")
        }

        // Chưa đủ tín hiệu chắc chắn -> trả về null để LLM xử lý
        return null
    }
}
