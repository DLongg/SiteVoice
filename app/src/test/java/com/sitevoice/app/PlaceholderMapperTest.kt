package com.sitevoice.app

import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.export.PlaceholderMapper
import com.sitevoice.app.template.DailyReportContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceholderMapperTest {

    @Test
    fun testExtractDocId() {
        val standardUrl = "https://docs.google.com/document/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit"
        assertEquals("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms", PlaceholderMapper.extractDocId(standardUrl))

        val driveUrl = "https://drive.google.com/file/d/1vP4n_Q16p_mK1Jp8u3BwzV1xZ/view"
        assertEquals("1vP4n_Q16p_mK1Jp8u3BwzV1xZ", PlaceholderMapper.extractDocId(driveUrl))

        val rawId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
        assertEquals(rawId, PlaceholderMapper.extractDocId(rawId))

        assertEquals("", PlaceholderMapper.extractDocId(""))
        assertEquals("", PlaceholderMapper.extractDocId("   "))
    }

    @Test
    fun testBuildPlaceholderMapWithCompleteData() {
        val dailyInput = DailyInputEntity(
            date = "2026-10-20",
            weatherMorning = "nang",
            weatherAfternoon = "mua",
            weatherEvening = "binh_thuong",
            commandStaffCount = 4,
            workerCount = 30,
            equipmentList = "1 máy xúc, 2 xe lu"
        )

        val reportContent = DailyReportContent(
            congViecThiCong = listOf("Đổ bê tông sàn tầng 3", "Lắp dựng cốt thép"),
            nghiemThu = listOf("Nghiệm thu ván khuôn sàn"),
            anToan = listOf("Trang bị đủ bảo hộ lao động"),
            yKienCdt = "Đẩy nhanh hoàn thiện tầng 2",
            yKienTvgs = "Bảo dưỡng ẩm bê tông đúng quy trình",
            yKienNhaThau = "Đề nghị cung cấp bản vẽ chi tiết",
            chuaPhanLoai = listOf("Ghi chú ngoài lề")
        )

        val map = PlaceholderMapper.buildPlaceholderMap("2026-10-20", dailyInput, reportContent)

        assertEquals("20", map["{{NGAY}}"])
        assertEquals("10", map["{{THANG}}"])
        assertEquals("2026", map["{{NAM}}"])
        assertEquals("20/10/2026", map["{{NGAY_THANG_NAM}}"])

        // Thời tiết
        assertEquals("☑", map["{{SANG_NANG}}"])
        assertEquals("☐", map["{{SANG_MUA}}"])
        assertEquals("☐", map["{{CHIEU_NANG}}"])
        assertEquals("☑", map["{{CHIEU_MUA}}"])
        assertEquals("☑", map["{{TOI_BINH_THUONG}}"])
        assertEquals("☐", map["{{TOI_MUA}}"])

        // Lực lượng
        assertEquals("4", map["{{BAN_CHI_HUY}}"])
        assertEquals("30", map["{{CONG_NHAN}}"])
        assertEquals("1 máy xúc, 2 xe lu", map["{{MAY_MOC}}"])

        // Danh sách gạch đầu dòng
        assertEquals("- Đổ bê tông sàn tầng 3\n- Lắp dựng cốt thép", map["{{CONG_VIEC_THI_CONG}}"])
        assertEquals("- Nghiệm thu ván khuôn sàn", map["{{NGHIEM_THU}}"])
        assertEquals("- Trang bị đủ bảo hộ lao động", map["{{AN_TOAN}}"])

        // Ý kiến
        assertEquals("Đẩy nhanh hoàn thiện tầng 2", map["{{Y_KIEN_CDT}}"])
        assertEquals("Bảo dưỡng ẩm bê tông đúng quy trình", map["{{Y_KIEN_TVGS}}"])
        assertEquals("Đề nghị cung cấp bản vẽ chi tiết", map["{{Y_KIEN_NHA_THAU}}"])

        // Đảm bảo chua_phan_loai không có placeholder trong doc
        assertTrue(!map.containsKey("{{CHUA_PHAN_LOAI}}"))
    }

    @Test
    fun testBuildPlaceholderMapWithCustomTemplate() {
        val customTemplate = TemplateItemEntity(
            id = "nhat-ky-ca-nhan",
            name = "Nhật ký cá nhân",
            description = "Báo cáo cá nhân",
            systemPrompt = "Tóm tắt cá nhân",
            schemaJson = """
                {
                    "summaryFields": [
                        {"name": "tam_trang", "type": "string"},
                        {"name": "cong_viec_hoan_thanh", "type": "list"},
                        {"name": "ton_dong_can_xu_ly", "type": "list"}
                    ]
                }
            """.trimIndent()
        )

        val reportContent = DailyReportContent(
            allFields = mapOf(
                "tam_trang" to "Phấn chấn, nhiều năng lượng",
                "cong_viec_hoan_thanh" to listOf("Xong tài liệu kiến trúc", "Build APK 11.94 MB"),
                "ton_dong_can_xu_ly" to listOf("Gửi bản vẽ cho kỹ sư")
            )
        )

        val map = PlaceholderMapper.buildPlaceholderMap("2026-10-20", null, reportContent, customTemplate)

        assertEquals("20", map["{{ngay}}"])
        assertEquals("Phấn chấn, nhiều năng lượng", map["{{tam_trang}}"])
        assertEquals("Phấn chấn, nhiều năng lượng", map["{{TAM_TRANG}}"])
        assertEquals("- Xong tài liệu kiến trúc\n- Build APK 11.94 MB", map["{{cong_viec_hoan_thanh}}"])
        assertEquals("- Gửi bản vẽ cho kỹ sư", map["{{ton_dong_can_xu_ly}}"])
    }

    @Test
    fun testBuildPlaceholderMapWithEmptyData() {
        val map = PlaceholderMapper.buildPlaceholderMap("2026-10-20", null, DailyReportContent())

        assertEquals("20", map["{{NGAY}}"])
        assertEquals("10", map["{{THANG}}"])
        assertEquals("2026", map["{{NAM}}"])
        assertEquals("0", map["{{BAN_CHI_HUY}}"])
        assertEquals("0", map["{{CONG_NHAN}}"])
        assertEquals("", map["{{MAY_MOC}}"])
        assertEquals("", map["{{CONG_VIEC_THI_CONG}}"])
        assertEquals("", map["{{NGHIEM_THU}}"])
        assertEquals("", map["{{AN_TOAN}}"])
        assertEquals("", map["{{Y_KIEN_CDT}}"])
        assertEquals("", map["{{Y_KIEN_TVGS}}"])
        assertEquals("", map["{{Y_KIEN_NHA_THAU}}"])
    }
}
