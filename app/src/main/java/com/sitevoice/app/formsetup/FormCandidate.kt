package com.sitevoice.app.formsetup

/**
 * Ứng viên chỗ điền được phát hiện bởi CODE từ cấu trúc Google Doc.
 * Tuân thủ docs/10-thiet-lap-form.md (Mục 20 & 33).
 */
data class FormCandidate(
    val id: Int,
    var startIndex: Int,
    var endIndex: Int,
    var truoc: String,
    var sau: String,
    var tinHieu: String, // "dong_cham", "o_tich", "doan_trong", "o_bang", "ngay", "the_ma"
    var tieuDeGanNhat: String,
    val soDongLienTiep: Int = 1,
    val loaiGoiY: String, // "van_ban", "danh_sach", "so", "ngay", "o_tich"
    var ma: String = "",
    var ten: String = "",
    var loai: String = "",
    var nguon: String = "",
    var boQua: Boolean = false,
    var doTinCay: Float = 1.0f,
    val originalMa: String = "" // Lưu mã gốc nếu quét từ the_ma
)
