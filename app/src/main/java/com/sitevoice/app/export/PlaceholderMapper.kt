package com.sitevoice.app.export

import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.template.DailyReportContent

object PlaceholderMapper {

    fun buildPlaceholderMap(
        date: String, // yyyy-MM-dd
        dailyInput: DailyInputEntity?,
        reportContent: DailyReportContent
    ): Map<String, String> {
        val dateParts = date.split("-")
        val year = dateParts.getOrNull(0) ?: ""
        val month = dateParts.getOrNull(1) ?: ""
        val day = dateParts.getOrNull(2) ?: ""

        val morningNang = if (dailyInput?.weatherMorning == "mua") "☐" else "☑"
        val morningMua = if (dailyInput?.weatherMorning == "mua") "☑" else "☐"

        val afternoonNang = if (dailyInput?.weatherAfternoon == "mua") "☐" else "☑"
        val afternoonMua = if (dailyInput?.weatherAfternoon == "mua") "☑" else "☐"

        val eveningBinhThuong = if (dailyInput?.weatherEvening == "mua") "☐" else "☑"
        val eveningMua = if (dailyInput?.weatherEvening == "mua") "☑" else "☐"

        val banChiHuy = (dailyInput?.commandStaffCount ?: 0).toString()
        val congNhan = (dailyInput?.workerCount ?: 0).toString()
        val mayMoc = dailyInput?.equipmentList?.trim() ?: ""

        val congViecThiCong = formatList(reportContent.congViecThiCong)
        val nghiemThu = formatList(reportContent.nghiemThu)
        val anToan = formatList(reportContent.anToan)

        val map = mutableMapOf<String, String>()
        map["{{NGAY}}"] = day
        map["{{THANG}}"] = month
        map["{{NAM}}"] = year

        map["{{SANG_NANG}}"] = morningNang
        map["{{SANG_MUA}}"] = morningMua
        map["{{CHIEU_NANG}}"] = afternoonNang
        map["{{CHIEU_MUA}}"] = afternoonMua
        map["{{TOI_BINH_THUONG}}"] = eveningBinhThuong
        map["{{TOI_MUA}}"] = eveningMua

        map["{{BAN_CHI_HUY}}"] = banChiHuy
        map["{{CONG_NHAN}}"] = congNhan
        map["{{MAY_MOC}}"] = mayMoc

        map["{{CONG_VIEC_THI_CONG}}"] = congViecThiCong
        map["{{NGHIEM_THU}}"] = nghiemThu
        map["{{AN_TOAN}}"] = anToan

        map["{{Y_KIEN_CDT}}"] = reportContent.yKienCdt.trim()
        map["{{Y_KIEN_TVGS}}"] = reportContent.yKienTvgs.trim()
        map["{{Y_KIEN_NHA_THAU}}"] = reportContent.yKienNhaThau.trim()

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
