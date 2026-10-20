package com.sitevoice.app.export

import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.template.DailyReportContent
import org.json.JSONObject
import java.util.regex.Pattern

object PlaceholderMapper {

    private val DOC_ID_PATTERN = Pattern.compile("/d/([a-zA-Z0-9_-]+)")

    fun extractDocId(urlOrId: String): String {
        val trimmed = urlOrId.trim()
        if (trimmed.isBlank()) return ""

        val matcher = DOC_ID_PATTERN.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1) ?: ""
        }

        // Nếu không chứa slash hoặc dấu chấm URL và không rỗng -> coi là ID trực tiếp
        if (!trimmed.contains("/") && !trimmed.contains(" ") && trimmed.length >= 10) {
            return trimmed
        }

        return ""
    }

    fun buildPlaceholderMap(
        date: String, // yyyy-MM-dd
        dailyInput: DailyInputEntity?,
        reportContent: DailyReportContent,
        template: TemplateItemEntity? = null
    ): Map<String, String> {
        val dateParts = date.split("-")
        val year = dateParts.getOrNull(0) ?: ""
        val month = dateParts.getOrNull(1) ?: ""
        val day = dateParts.getOrNull(2) ?: ""

        val map = mutableMapOf<String, String>()

        // 1. Ngày tháng (cả hoa lẫn thường)
        map["{{NGAY}}"] = day
        map["{{ngay}}"] = day
        map["{{THANG}}"] = month
        map["{{thang}}"] = month
        map["{{NAM}}"] = year
        map["{{nam}}"] = year
        map["{{NGAY_THANG_NAM}}"] = "$day/$month/$year"
        map["{{ngay_thang_nam}}"] = "$day/$month/$year"
        map["{{DATE}}"] = date
        map["{{date}}"] = date

        // 2. Dữ liệu thời tiết & nhân lực chuẩn công trường
        val morningNang = if (dailyInput?.weatherMorning == "mua") "☐" else "☑"
        val morningMua = if (dailyInput?.weatherMorning == "mua") "☑" else "☐"

        val afternoonNang = if (dailyInput?.weatherAfternoon == "mua") "☐" else "☑"
        val afternoonMua = if (dailyInput?.weatherAfternoon == "mua") "☑" else "☐"

        val eveningBinhThuong = if (dailyInput?.weatherEvening == "mua") "☐" else "☑"
        val eveningMua = if (dailyInput?.weatherEvening == "mua") "☑" else "☐"

        val banChiHuy = (dailyInput?.commandStaffCount ?: 0).toString()
        val congNhan = (dailyInput?.workerCount ?: 0).toString()
        val mayMoc = dailyInput?.equipmentList?.trim() ?: ""

        map["{{SANG_NANG}}"] = morningNang
        map["{{SANG_MUA}}"] = morningMua
        map["{{CHIEU_NANG}}"] = afternoonNang
        map["{{CHIEU_MUA}}"] = afternoonMua
        map["{{TOI_BINH_THUONG}}"] = eveningBinhThuong
        map["{{TOI_MUA}}"] = eveningMua

        map["{{BAN_CHI_HUY}}"] = banChiHuy
        map["{{CONG_NHAN}}"] = congNhan
        map["{{MAY_MOC}}"] = mayMoc

        // 3. Các trường chuẩn của Nhật ký công trường
        val congViecThiCong = formatList(reportContent.congViecThiCong)
        val nghiemThu = formatList(reportContent.nghiemThu)
        val anToan = formatList(reportContent.anToan)

        map["{{CONG_VIEC_THI_CONG}}"] = congViecThiCong
        map["{{NGHIEM_THU}}"] = nghiemThu
        map["{{AN_TOAN}}"] = anToan

        map["{{Y_KIEN_CDT}}"] = reportContent.yKienCdt.trim()
        map["{{Y_KIEN_TVGS}}"] = reportContent.yKienTvgs.trim()
        map["{{Y_KIEN_NHA_THAU}}"] = reportContent.yKienNhaThau.trim()

        // 4. Ánh xạ động từ allFields của reportContent
        for ((key, value) in reportContent.allFields) {
            val formattedValue = when (value) {
                is List<*> -> formatList(value.map { it?.toString() ?: "" })
                is String -> value.trim()
                else -> value.toString()
            }
            map["{{$key}}"] = formattedValue
            map["{{${key.uppercase()}}}"] = formattedValue
        }

        // 5. Nếu có template, bổ sung các trường được định nghĩa trong schema nếu chưa có
        if (template != null && template.schemaJson.isNotBlank()) {
            try {
                val schemaObj = JSONObject(template.schemaJson)
                val summaryFields = schemaObj.optJSONArray("summaryFields")
                if (summaryFields != null) {
                    for (i in 0 until summaryFields.length()) {
                        val fieldObj = summaryFields.optJSONObject(i) ?: continue
                        val fieldName = fieldObj.optString("name")
                        if (fieldName.isNotBlank()) {
                            val upperPlaceholder = "{{${fieldName.uppercase()}}}"
                            val exactPlaceholder = "{{$fieldName}}"
                            if (!map.containsKey(upperPlaceholder)) {
                                map[upperPlaceholder] = ""
                            }
                            if (!map.containsKey(exactPlaceholder)) {
                                map[exactPlaceholder] = ""
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // schema parse fallback
            }
        }

        return map
    }

    private fun formatList(items: List<String>): String {
        val filtered = items.map { it.trim() }.filter { it.isNotBlank() }
        if (filtered.isEmpty()) return ""
        return filtered.joinToString("\n") { line ->
            if (line.startsWith("- ")) line else "- $line"
        }
    }
}
