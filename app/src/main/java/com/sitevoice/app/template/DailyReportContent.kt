package com.sitevoice.app.template

import org.json.JSONObject

data class DailyReportContent(
    val congViecThiCong: List<String> = emptyList(),
    val nghiemThu: List<String> = emptyList(),
    val anToan: List<String> = emptyList(),
    val yKienCdt: String = "",
    val yKienTvgs: String = "",
    val yKienNhaThau: String = "",
    val chuaPhanLoai: List<String> = emptyList()
) {
    companion object {
        fun fromJson(jsonStr: String): DailyReportContent {
            if (jsonStr.isBlank()) return DailyReportContent()
            return try {
                val json = JSONObject(jsonStr)
                val congViecThiCong = json.optJSONArray("cong_viec_thi_cong")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()

                val nghiemThu = json.optJSONArray("nghiem_thu")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()

                val anToan = json.optJSONArray("an_toan")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()

                val yKienCdt = json.optString("y_kien_cdt", "")
                val yKienTvgs = json.optString("y_kien_tvgs", "")
                val yKienNhaThau = json.optString("y_kien_nha_thau", "")

                val chuaPhanLoai = json.optJSONArray("chua_phan_loai")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()

                DailyReportContent(
                    congViecThiCong = congViecThiCong,
                    nghiemThu = nghiemThu,
                    anToan = anToan,
                    yKienCdt = yKienCdt,
                    yKienTvgs = yKienTvgs,
                    yKienNhaThau = yKienNhaThau,
                    chuaPhanLoai = chuaPhanLoai
                )
            } catch (e: Exception) {
                DailyReportContent()
            }
        }
    }
}
