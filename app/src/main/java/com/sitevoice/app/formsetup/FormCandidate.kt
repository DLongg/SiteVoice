package com.sitevoice.app.formsetup

/**
 * Ứng viên chỗ điền được phát hiện bởi CODE từ cấu trúc Google Doc.
 * Tuân thủ docs/10-thiet-lap-form.md (Mục 20 & 33).
 */
data class FormCandidate(
    val id: Int,
    val startIndex: Int,
    val endIndex: Int,
    val truoc: String,
    val sau: String,
    val tinHieu: String, // "dong_cham", "o_tich", "doan_trong", "o_bang", "ngay"
    val tieuDeGanNhat: String,
    val soDongLienTiep: Int = 1,
    val loaiGoiY: String, // "van_ban", "danh_sach", "so", "ngay", "o_tich"
    var ma: String = "",
    var ten: String = "",
    var loai: String = "",
    var nguon: String = "",
    var boQua: Boolean = false,
    var doTinCay: Float = 1.0f
)
