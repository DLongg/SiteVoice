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

                // Thu thập tất cả các trường động (làm phẳng các JSONObject lồng nhau)
                val dynamicMap = mutableMapOf<String, Any>()
                fun extractFields(obj: JSONObject) {
                    val itKeys = obj.keys()
                    while (itKeys.hasNext()) {
                        val key = itKeys.next()
                        val value = obj.get(key)
                        when (value) {
                            is JSONObject -> {
                                dynamicMap[key] = value.toString()
                                extractFields(value) // Đệ quy làm phẳng
                            }
                            is JSONArray -> {
                                val list = (0 until value.length()).map { value.getString(it) }
                                dynamicMap[key] = list
                            }
                            is Boolean -> dynamicMap[key] = value
                            is Number -> dynamicMap[key] = value.toString()
                            is String -> dynamicMap[key] = value
                            else -> dynamicMap[key] = value
                        }
                    }
                }
                extractFields(json)

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
