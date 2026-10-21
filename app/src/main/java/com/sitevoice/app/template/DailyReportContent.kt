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

                // Backward-compatible fields cho mẫu công trường (hỗ trợ cả JSONArray lẫn multiline String)
                val congViecThiCong = extractList(
                    json,
                    "cong_viec_thi_cong",
                    "thi_cong_trong_ngay",
                    "thi_cong",
                    "cong_viec",
                    "work_done",
                    "CONG_VIEC_THI_CONG",
                    "THI_CONG_TRONG_NGAY"
                )

                val nghiemThu = extractList(
                    json,
                    "nghiem_thu",
                    "nghiem_thu_trong_ngay",
                    "nghiem_thu_cong_viec",
                    "NGHIEM_THU",
                    "NGHIEM_THU_TRONG_NGAY"
                )

                val anToan = extractList(
                    json,
                    "an_toan",
                    "an_toan_vs",
                    "an_toan_lao_dong",
                    "an_toan_ve_sinh",
                    "AN_TOAN",
                    "AN_TOAN_LAO_DONG"
                )

                val yKienCdt = extractString(json, "y_kien_cdt", "yk_cdt", "chu_dau_tu", "y_kien_chu_dau_tu", "Y_KIEN_CDT")
                val yKienTvgs = extractString(json, "y_kien_tvgs", "yk_tvgs", "tu_van_giam_sat", "y_kien_tu_van_giam_sat", "Y_KIEN_TVGS")
                val yKienNhaThau = extractString(json, "y_kien_nha_thau", "yk_nha_thau", "nha_thau", "y_kien_ban_chi_huy", "Y_KIEN_NHA_THAU")

                val chuaPhanLoai = extractList(json, "chua_phan_loai", "unclassified", "ghi_chu_khac", "CHUA_PHAN_LOAI")

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

        private fun extractList(json: JSONObject, vararg keys: String): List<String> {
            for (key in keys) {
                if (!json.has(key)) continue
                val value = json.opt(key) ?: continue
                when (value) {
                    is JSONArray -> {
                        val list = mutableListOf<String>()
                        for (i in 0 until value.length()) {
                            val item = value.optString(i, "")
                            if (item.isNotBlank()) {
                                if (item.contains("\n")) {
                                    list.addAll(item.split("\n").map { it.trimEnd() }.filter { it.isNotBlank() })
                                } else {
                                    list.add(item.trimEnd())
                                }
                            }
                        }
                        if (list.isNotEmpty()) return list
                    }
                    is String -> {
                        val trimmed = value.trim()
                        if (trimmed.isNotBlank()) {
                            val lines = trimmed.split("\n").map { it.trimEnd() }.filter { it.isNotBlank() }
                            if (lines.isNotEmpty()) return lines
                        }
                    }
                    else -> {}
                }
            }
            return emptyList()
        }

        private fun extractString(json: JSONObject, vararg keys: String): String {
            for (key in keys) {
                if (!json.has(key)) continue
                val value = json.opt(key) ?: continue
                when (value) {
                    is String -> {
                        if (value.isNotBlank()) return value.trim()
                    }
                    is JSONArray -> {
                        val list = (0 until value.length()).map { value.optString(it, "").trim() }.filter { it.isNotBlank() }
                        if (list.isNotEmpty()) return list.joinToString("\n")
                    }
                    else -> {
                        val str = value.toString().trim()
                        if (str.isNotBlank()) return str
                    }
                }
            }
            return ""
        }
    }
}
