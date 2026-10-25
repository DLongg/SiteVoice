package com.sitevoice.app.export

import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.DailyReportEntity
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.template.DailyReportContent
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

data class DailyLogEntity(
    val date: String = "",
    val dailyInput: DailyInputEntity? = null,
    val reportContent: DailyReportContent = DailyReportContent(),
    val contentJson: String = "",
    val templateId: String = ""
) {
    constructor(report: DailyReportEntity, dailyInput: DailyInputEntity? = null) : this(
        date = report.date,
        dailyInput = dailyInput,
        reportContent = DailyReportContent.fromJson(report.contentJson),
        contentJson = report.contentJson,
        templateId = report.templateId
    )
}

object PlaceholderMapper {

    fun mapToPlaceholders(
        log: DailyLogEntity,
        template: TemplateItemEntity
    ): Map<String, String> {
        val content = if (log.contentJson.isNotBlank() && log.reportContent.allFields.isEmpty()) {
            DailyReportContent.fromJson(log.contentJson)
        } else {
            log.reportContent
        }
        return buildPlaceholderMap(
            date = log.date,
            dailyInput = log.dailyInput,
            reportContent = content,
            template = template
        )
    }

    fun mapToPlaceholders(
        report: DailyReportEntity,
        template: TemplateItemEntity,
        dailyInput: DailyInputEntity? = null
    ): Map<String, String> {
        return buildPlaceholderMap(
            date = report.date,
            dailyInput = dailyInput,
            reportContent = DailyReportContent.fromJson(report.contentJson),
            template = template
        )
    }

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
        val inputEquipment = dailyInput?.equipmentList?.trim().orEmpty()
        val noteEquipment = when (val eqObj = reportContent.allFields["may_moc"] ?: reportContent.allFields["thiet_bi"]) {
            is List<*> -> formatList(eqObj.map { it?.toString() ?: "" })
            is String -> eqObj.trim()
            null -> ""
            else -> eqObj.toString().trim()
        }
        val mayMoc = if (inputEquipment.isNotBlank()) inputEquipment else noteEquipment

        map["{{SANG_NANG}}"] = morningNang
        map["{{SANG_MUA}}"] = morningMua
        map["{{CHIEU_NANG}}"] = afternoonNang
        map["{{CHIEU_MUA}}"] = afternoonMua
        map["{{TOI_BINH_THUONG}}"] = eveningBinhThuong
        map["{{TOI_MUA}}"] = eveningMua

        map["{{BAN_CHI_HUY}}"] = banChiHuy
        map["{{CONG_NHAN}}"] = congNhan
        map["{{MAY_MOC}}"] = mayMoc
        map["{{may_moc}}"] = mayMoc
        map["{{THIET_BI}}"] = mayMoc
        map["{{thiet_bi}}"] = mayMoc

        // Các alias phổ biến từ mẫu tự tạo / viết tắt
        map["{{s_n}}"] = morningNang
        map["{{s_m}}"] = morningMua
        map["{{c_n}}"] = afternoonNang
        map["{{c_m}}"] = afternoonMua
        map["{{t_b}}"] = eveningBinhThuong
        map["{{t_bt}}"] = eveningBinhThuong
        map["{{t_m}}"] = eveningMua
        map["{{tong_quan_thoi_tiet}}"] = "Sáng: ${if (morningNang == "☑") "nắng" else "mưa"}, Chiều: ${if (afternoonNang == "☑") "nắng" else "mưa"}, Tối: ${if (eveningBinhThuong == "☑") "bình thường" else "mưa"}"

        map["{{ng_ch}}"] = banChiHuy
        map["{{ng_cn}}"] = congNhan

        // 3. Các trường chuẩn của Nhật ký công trường
        val congViecThiCong = formatList(reportContent.congViecThiCong)
        val nghiemThu = formatList(reportContent.nghiemThu)
        val anToan = formatList(reportContent.anToan)

        map["{{CONG_VIEC_THI_CONG}}"] = congViecThiCong
        map["{{cong_viec_thi_cong}}"] = congViecThiCong
        map["{{THI_CONG_TRONG_NGAY}}"] = congViecThiCong
        map["{{thi_cong_trong_ngay}}"] = congViecThiCong
        map["{{THI_CONG}}"] = congViecThiCong
        map["{{thi_cong}}"] = congViecThiCong
        map["{{CONG_VIEC}}"] = congViecThiCong
        map["{{cong_viec}}"] = congViecThiCong

        map["{{NGHIEM_THU}}"] = nghiemThu
        map["{{nghiem_thu}}"] = nghiemThu
        map["{{NGHIEM_THU_TRONG_NGAY}}"] = nghiemThu
        map["{{nghiem_thu_trong_ngay}}"] = nghiemThu

        map["{{AN_TOAN}}"] = anToan
        map["{{an_toan}}"] = anToan
        map["{{AN_TOAN_LAO_DONG}}"] = anToan
        map["{{an_toan_lao_dong}}"] = anToan
        map["{{AN_TOAN_VS}}"] = anToan
        map["{{an_toan_vs}}"] = anToan

        map["{{Y_KIEN_CDT}}"] = reportContent.yKienCdt.trim()
        map["{{y_kien_cdt}}"] = reportContent.yKienCdt.trim()
        map["{{YK_CDT}}"] = reportContent.yKienCdt.trim()
        map["{{yk_cdt}}"] = reportContent.yKienCdt.trim()
        map["{{Y_KIEN_CHU_DAU_TU}}"] = reportContent.yKienCdt.trim()
        map["{{y_kien_chu_dau_tu}}"] = reportContent.yKienCdt.trim()

        map["{{Y_KIEN_TVGS}}"] = reportContent.yKienTvgs.trim()
        map["{{y_kien_tvgs}}"] = reportContent.yKienTvgs.trim()
        map["{{YK_TVGS}}"] = reportContent.yKienTvgs.trim()
        map["{{yk_tvgs}}"] = reportContent.yKienTvgs.trim()
        map["{{Y_KIEN_TU_VAN_GIAM_SAT}}"] = reportContent.yKienTvgs.trim()
        map["{{y_kien_tu_van_giam_sat}}"] = reportContent.yKienTvgs.trim()

        map["{{Y_KIEN_NHA_THAU}}"] = reportContent.yKienNhaThau.trim()
        map["{{y_kien_nha_thau}}"] = reportContent.yKienNhaThau.trim()
        map["{{YK_NHA_THAU}}"] = reportContent.yKienNhaThau.trim()
        map["{{yk_nha_thau}}"] = reportContent.yKienNhaThau.trim()

        // 4. Ánh xạ động từ allFields của reportContent
        for ((key, value) in reportContent.allFields) {
            val formattedValue = when (value) {
                is List<*> -> formatList(value.map { it?.toString() ?: "" })
                is Boolean -> if (value) "☑" else "☐"
                is String -> {
                    if (value.equals("true", ignoreCase = true)) "☑"
                    else if (value.equals("false", ignoreCase = true)) "☐"
                    else {
                        if (value.contains("\n")) {
                            formatList(value.split("\n"))
                        } else {
                            value.trim()
                        }
                    }
                }
                else -> value.toString()
            }
            map["{{$key}}"] = formattedValue
            map["{{${key.uppercase()}}}"] = formattedValue
            map["{{${key.lowercase()}}}"] = formattedValue

            when (key.lowercase()) {
                "cong_viec_thi_cong", "thi_cong_trong_ngay", "thi_cong", "cong_viec" -> {
                    map["{{cong_viec_thi_cong}}"] = formattedValue
                    map["{{CONG_VIEC_THI_CONG}}"] = formattedValue
                    map["{{thi_cong_trong_ngay}}"] = formattedValue
                    map["{{THI_CONG_TRONG_NGAY}}"] = formattedValue
                    map["{{thi_cong}}"] = formattedValue
                    map["{{THI_CONG}}"] = formattedValue
                    map["{{cong_viec}}"] = formattedValue
                    map["{{CONG_VIEC}}"] = formattedValue
                }
                "nghiem_thu", "nghiem_thu_trong_ngay", "nghiem_thu_cong_viec" -> {
                    map["{{nghiem_thu}}"] = formattedValue
                    map["{{NGHIEM_THU}}"] = formattedValue
                    map["{{nghiem_thu_trong_ngay}}"] = formattedValue
                    map["{{NGHIEM_THU_TRONG_NGAY}}"] = formattedValue
                }
                "an_toan", "an_toan_vs", "an_toan_lao_dong", "an_toan_ve_sinh" -> {
                    map["{{an_toan}}"] = formattedValue
                    map["{{AN_TOAN}}"] = formattedValue
                    map["{{an_toan_lao_dong}}"] = formattedValue
                    map["{{AN_TOAN_LAO_DONG}}"] = formattedValue
                    map["{{an_toan_vs}}"] = formattedValue
                    map["{{AN_TOAN_VS}}"] = formattedValue
                }
                "y_kien_cdt", "yk_cdt", "chu_dau_tu", "y_kien_chu_dau_tu" -> {
                    map["{{y_kien_cdt}}"] = formattedValue
                    map["{{Y_KIEN_CDT}}"] = formattedValue
                    map["{{yk_cdt}}"] = formattedValue
                    map["{{YK_CDT}}"] = formattedValue
                    map["{{y_kien_chu_dau_tu}}"] = formattedValue
                    map["{{Y_KIEN_CHU_DAU_TU}}"] = formattedValue
                }
                "y_kien_tvgs", "yk_tvgs", "tu_van_giam_sat", "y_kien_tu_van_giam_sat" -> {
                    map["{{y_kien_tvgs}}"] = formattedValue
                    map["{{Y_KIEN_TVGS}}"] = formattedValue
                    map["{{yk_tvgs}}"] = formattedValue
                    map["{{YK_TVGS}}"] = formattedValue
                    map["{{y_kien_tu_van_giam_sat}}"] = formattedValue
                    map["{{Y_KIEN_TU_VAN_GIAM_SAT}}"] = formattedValue
                }
                "y_kien_nha_thau", "yk_nha_thau", "nha_thau", "y_kien_ban_chi_huy" -> {
                    map["{{y_kien_nha_thau}}"] = formattedValue
                    map["{{Y_KIEN_NHA_THAU}}"] = formattedValue
                    map["{{yk_nha_thau}}"] = formattedValue
                    map["{{YK_NHA_THAU}}"] = formattedValue
                }
                "s_n", "sang_nang" -> {
                    map["{{s_n}}"] = formattedValue
                    map["{{S_N}}"] = formattedValue
                    map["{{sang_nang}}"] = formattedValue
                    map["{{SANG_NANG}}"] = formattedValue
                }
                "s_m", "sang_mua" -> {
                    map["{{s_m}}"] = formattedValue
                    map["{{S_M}}"] = formattedValue
                    map["{{sang_mua}}"] = formattedValue
                    map["{{SANG_MUA}}"] = formattedValue
                }
                "c_n", "chieu_nang" -> {
                    map["{{c_n}}"] = formattedValue
                    map["{{C_N}}"] = formattedValue
                    map["{{chieu_nang}}"] = formattedValue
                    map["{{CHIEU_NANG}}"] = formattedValue
                }
                "c_m", "chieu_mua" -> {
                    map["{{c_m}}"] = formattedValue
                    map["{{C_M}}"] = formattedValue
                    map["{{chieu_mua}}"] = formattedValue
                    map["{{CHIEU_MUA}}"] = formattedValue
                }
                "t_b", "t_bt", "toi_binh_thuong" -> {
                    map["{{t_b}}"] = formattedValue
                    map["{{T_B}}"] = formattedValue
                    map["{{t_bt}}"] = formattedValue
                    map["{{T_BT}}"] = formattedValue
                    map["{{toi_binh_thuong}}"] = formattedValue
                    map["{{TOI_BINH_THUONG}}"] = formattedValue
                }
                "t_m", "toi_mua" -> {
                    map["{{t_m}}"] = formattedValue
                    map["{{T_M}}"] = formattedValue
                    map["{{toi_mua}}"] = formattedValue
                    map["{{TOI_MUA}}"] = formattedValue
                }
                "ng_ch", "ban_chi_huy" -> {
                    map["{{ng_ch}}"] = formattedValue
                    map["{{NG_CH}}"] = formattedValue
                    map["{{ban_chi_huy}}"] = formattedValue
                    map["{{BAN_CHI_HUY}}"] = formattedValue
                }
                "ng_cn", "cong_nhan" -> {
                    map["{{ng_cn}}"] = formattedValue
                    map["{{NG_CN}}"] = formattedValue
                    map["{{cong_nhan}}"] = formattedValue
                    map["{{CONG_NHAN}}"] = formattedValue
                }
                "may_moc", "thiet_bi" -> {
                    if (inputEquipment.isBlank() || formattedValue.isNotBlank()) {
                        val finalVal = if (inputEquipment.isNotBlank()) inputEquipment else formattedValue
                        map["{{may_moc}}"] = finalVal
                        map["{{MAY_MOC}}"] = finalVal
                        map["{{thiet_bi}}"] = finalVal
                        map["{{THIET_BI}}"] = finalVal
                    }
                }
            }
        }

        // 5. Nếu có template, nạp toàn bộ các trường được định nghĩa trong schemaJson với giá trị mặc định "" nếu chưa có
        if (template != null && template.schemaJson.isNotBlank()) {
            try {
                val schemaObj = JSONObject(template.schemaJson)

                fun registerTag(rawTag: String) {
                    val trimmed = rawTag.trim()
                    if (trimmed.isBlank()) return
                    val inner = if (trimmed.startsWith("{{") && trimmed.endsWith("}}")) {
                        trimmed.substring(2, trimmed.length - 2).trim()
                    } else {
                        trimmed
                    }
                    if (inner.isBlank()) return

                    val baseTag = "{{$inner}}"
                    val upperTag = "{{${inner.uppercase()}}}"
                    val lowerTag = "{{${inner.lowercase()}}}"

                    if (!map.containsKey(baseTag)) map[baseTag] = ""
                    if (!map.containsKey(upperTag)) map[upperTag] = ""
                    if (!map.containsKey(lowerTag)) map[lowerTag] = ""
                }

                fun processFieldObject(fieldObj: JSONObject?) {
                    if (fieldObj == null) return
                    fieldObj.optString("placeholder").takeIf { it.isNotBlank() }?.let { registerTag(it) }
                    fieldObj.optString("key").takeIf { it.isNotBlank() }?.let { registerTag(it) }
                    fieldObj.optString("name").takeIf { it.isNotBlank() }?.let { registerTag(it) }
                    fieldObj.optString("ma").takeIf { it.isNotBlank() }?.let { registerTag(it) }
                }

                fun processContainer(obj: Any?) {
                    when (obj) {
                        is JSONObject -> {
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                registerTag(key)
                                val child = obj.opt(key)
                                if (child is JSONObject) {
                                    processFieldObject(child)
                                } else if (child is String && child.isNotBlank()) {
                                    registerTag(child)
                                }
                            }
                        }
                        is JSONArray -> {
                            for (i in 0 until obj.length()) {
                                val item = obj.opt(i)
                                when (item) {
                                    is JSONObject -> processFieldObject(item)
                                    is String -> registerTag(item)
                                }
                            }
                        }
                    }
                }

                // 1. truong_tong_hop (JSONObject hoặc JSONArray)
                processContainer(schemaObj.opt("truong_tong_hop"))

                // 2. nhap_tay (JSONObject hoặc JSONArray)
                processContainer(schemaObj.opt("nhap_tay"))

                // 3. summary_fields / summaryFields (JSONArray hoặc JSONObject)
                processContainer(schemaObj.opt("summary_fields") ?: schemaObj.opt("summaryFields"))

                // 4. manual_fields / manualFields (JSONArray hoặc JSONObject)
                processContainer(schemaObj.opt("manual_fields") ?: schemaObj.opt("manualFields"))

                // 5. cho_dien (JSONArray hoặc JSONObject)
                processContainer(schemaObj.opt("cho_dien"))

            } catch (_: Exception) {
                // schema parse fallback
            }
        }

        // 6. Mở rộng các biến thể có khoảng trắng trong ngoặc {{ KEY }} để tương thích tối đa với Google Doc
        val expandedMap = mutableMapOf<String, String>()
        for ((placeholder, value) in map) {
            expandedMap[placeholder] = value
            if (placeholder.startsWith("{{") && placeholder.endsWith("}}")) {
                val inner = placeholder.substring(2, placeholder.length - 2).trim()
                expandedMap["{{$inner}}"] = value
                expandedMap["{{ $inner }}"] = value
                expandedMap["{{$inner }}"] = value
                expandedMap["{{ $inner}}"] = value
            }
        }

        return expandedMap
    }

    fun formatList(items: List<String>): String {
        val flattened = items.flatMap { it.split("\n") }.filter { it.isNotBlank() }
        if (flattened.isEmpty()) return ""

        var hasSeenHeader = false
        val resultLines = mutableListOf<String>()

        for (rawLine in flattened) {
            val line = rawLine.trimEnd()
            val trimmedStart = line.trimStart()
            // Tiêu đề phân nhóm / hạng mục: "3.1 Điện", "3.2 Nước", "1.", "1.1", "A."
            val isHeader = trimmedStart.matches(Regex("""^(\d+(\.\d+)*|[A-Z]\.|\d+\))\s+.*"""))

            if (isHeader) {
                hasSeenHeader = true
                resultLines.add(trimmedStart)
            } else {
                val isAlreadyBulleted = trimmedStart.startsWith("- ") ||
                        trimmedStart.startsWith("• ") ||
                        trimmedStart.startsWith("* ") ||
                        trimmedStart.startsWith("+ ")
                val cleanText = if (isAlreadyBulleted) {
                    trimmedStart.removePrefix("- ")
                        .removePrefix("• ")
                        .removePrefix("* ")
                        .removePrefix("+ ")
                        .trimStart()
                } else {
                    trimmedStart
                }

                if (line.startsWith("\t") || line.startsWith("  ")) {
                    // Đã thụt đầu dòng sẵn trong chuỗi
                    resultLines.add("\t- $cleanText")
                } else if (hasSeenHeader) {
                    // Nằm dưới nhóm hạng mục (3.1, 3.2...) -> thụt lề bằng tab
                    resultLines.add("\t- $cleanText")
                } else {
                    // Đầu mục thông thường -> gạch đầu dòng tiêu chuẩn
                    resultLines.add("- $cleanText")
                }
            }
        }
        return resultLines.joinToString("\n")
    }
}
