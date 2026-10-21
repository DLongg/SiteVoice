package com.sitevoice.app.template

import org.json.JSONArray
import org.json.JSONObject

data class DailyReportContent(
    val congViecThiCong: List<String> = emptyList(),
    val nghiemThu: List<String> = emptyList(),
    val anToan: List<String> = emptyList(),
    val yKienCdt: String = "",
    val yKienTvgs: String = "",
    val yKienNhaThau: String = "",
    val chuaPhanLoai: List<String> = emptyList(),
    val rawJson: String = "",
    val allFields: Map<String, Any> = emptyMap()
) {
    companion object {
        fun fromJson(jsonStr: String): DailyReportContent {
            if (jsonStr.isBlank()) return DailyReportContent()
            return try {
                val json = JSONObject(jsonStr)

                // Backward-compatible fields cho mẫu công trường
                val congViecThiCong = (json.optJSONArray("cong_viec_thi_cong")
                    ?: json.optJSONArray("thi_cong_trong_ngay"))?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()

                val nghiemThu = (json.optJSONArray("nghiem_thu")
                    ?: json.optJSONArray("nghiem_thu_trong_ngay"))?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()

                val anToan = (json.optJSONArray("an_toan")
                    ?: json.optJSONArray("an_toan_vs"))?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()

                val yKienCdt = if (json.has("y_kien_cdt")) json.optString("y_kien_cdt", "") else json.optString("yk_cdt", "")
                val yKienTvgs = if (json.has("y_kien_tvgs")) json.optString("y_kien_tvgs", "") else json.optString("yk_tvgs", "")
                val yKienNhaThau = if (json.has("y_kien_nha_thau")) json.optString("y_kien_nha_thau", "") else json.optString("yk_nha_thau", "")

                val chuaPhanLoai = json.optJSONArray("chua_phan_loai")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()

                // Thu thập tất cả các trường động
                val dynamicMap = mutableMapOf<String, Any>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = json.get(key)
                    if (value is JSONArray) {
                        val list = (0 until value.length()).map { value.getString(it) }
                        dynamicMap[key] = list
                    } else if (value is Boolean) {
                        dynamicMap[key] = value
                    } else if (value is String) {
                        dynamicMap[key] = value
                    } else {
                        dynamicMap[key] = value
                    }
                }

                DailyReportContent(
                    congViecThiCong = congViecThiCong,
                    nghiemThu = nghiemThu,
                    anToan = anToan,
                    yKienCdt = yKienCdt,
                    yKienTvgs = yKienTvgs,
                    yKienNhaThau = yKienNhaThau,
                    chuaPhanLoai = chuaPhanLoai,
                    rawJson = jsonStr,
                    allFields = dynamicMap
                )
            } catch (e: Exception) {
                DailyReportContent()
            }
        }
    }
}
