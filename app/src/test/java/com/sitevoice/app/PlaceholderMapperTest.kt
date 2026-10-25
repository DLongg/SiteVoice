package com.sitevoice.app

import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.export.DailyLogEntity
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

    @Test
    fun testSectionFormattingAndAliases() {
        val content = DailyReportContent(
            congViecThiCong = listOf(
                "3.1 Điện",
                "lắp đặt ống điện âm tường tầng 8",
                "Chôn đế , tủ điện tầng 8",
                "3.2 Nước",
                "- Thi công trục thoát nước 10,  11 (lô gia)",
                "- Thăm ống, bể tự hoại tầng 1"
            )
        )

        val map = PlaceholderMapper.buildPlaceholderMap("2026-10-21", null, content)

        val expected = """3.1 Điện
	- lắp đặt ống điện âm tường tầng 8
	- Chôn đế , tủ điện tầng 8
3.2 Nước
	- Thi công trục thoát nước 10,  11 (lô gia)
	- Thăm ống, bể tự hoại tầng 1"""

        assertEquals(expected, map["{{CONG_VIEC_THI_CONG}}"])
        assertEquals(expected, map["{{thi_cong_trong_ngay}}"])
        assertEquals(expected, map["{{ thi_cong_trong_ngay }}"])
        assertEquals(expected, map["{{cong_viec}}"])
    }

    @Test
    fun testMapToPlaceholdersVietnameseSchemaJsonObjectDefaultsToEmpty() {
        val vnTemplate = TemplateItemEntity(
            id = "nhat-ky-cong-truong",
            name = "Nhật ký thi công",
            description = "Mẫu chuẩn",
            systemPrompt = "Prompt",
            schemaJson = """
                {
                    "ten": "Nhật ký công trường",
                    "truong_tong_hop": {
                        "cong_viec_thi_cong": { "kieu": "danh_sach" },
                        "nghiem_thu": { "kieu": "danh_sach" },
                        "an_toan": { "kieu": "danh_sach" },
                        "tong_quan": { "kieu": "van_ban", "placeholder": "{{TONG_QUAN}}" }
                    },
                    "nhap_tay": {
                        "ngay": { "kieu": "ngay" },
                        "thang": { "kieu": "so" },
                        "s_n": { "kieu": "o_tich" },
                        "nguoi_lap_bieu": { "kieu": "chu", "placeholder": "{{NGUOI_LAP_BIEU}}" }
                    }
                }
            """.trimIndent()
        )

        val log = DailyLogEntity(date = "2026-10-24")
        val map = PlaceholderMapper.mapToPlaceholders(log, vnTemplate)

        // Các trường tổng hợp không có dữ liệu phải mặc định là ""
        assertEquals("", map["{{cong_viec_thi_cong}}"])
        assertEquals("", map["{{CONG_VIEC_THI_CONG}}"])
        assertEquals("", map["{{nghiem_thu}}"])
        assertEquals("", map["{{NGHIEM_THU}}"])
        assertEquals("", map["{{an_toan}}"])
        assertEquals("", map["{{AN_TOAN}}"])
        assertEquals("", map["{{tong_quan}}"])
        assertEquals("", map["{{TONG_QUAN}}"])
        assertEquals("", map["{{ tong_quan }}"])
        assertEquals("", map["{{ TONG_QUAN }}"])

        // Trường nhập tay chưa điền dữ liệu
        assertEquals("", map["{{nguoi_lap_bieu}}"])
        assertEquals("", map["{{NGUOI_LAP_BIEU}}"])
        assertEquals("", map["{{ NGUOI_LAP_BIEU }}"])
    }

    @Test
    fun testMapToPlaceholdersVietnameseSchemaJsonArrayDefaultsToEmpty() {
        val arrayTemplate = TemplateItemEntity(
            id = "kiem-tra-hien-truong",
            name = "Kiểm tra hiện trường",
            description = "Mẫu mảng",
            systemPrompt = "Prompt",
            schemaJson = """
                {
                    "ten": "Biên bản kiểm tra",
                    "truong_tong_hop": [
                        { "ma": "HANG_MUC", "kieu": "danh_sach", "placeholder": "{{HANG_MUC}}" },
                        { "ma": "KET_QUA", "kieu": "van_ban" }
                    ],
                    "nhap_tay": ["ngay_kiem_tra", "can_bo_giam_sat"]
                }
            """.trimIndent()
        )

        val log = DailyLogEntity(date = "2026-10-24")
        val map = PlaceholderMapper.mapToPlaceholders(log, arrayTemplate)

        assertEquals("", map["{{HANG_MUC}}"])
        assertEquals("", map["{{hang_muc}}"])
        assertEquals("", map["{{ HANG_MUC }}"])
        assertEquals("", map["{{KET_QUA}}"])
        assertEquals("", map["{{ket_qua}}"])
        assertEquals("", map["{{ngay_kiem_tra}}"])
        assertEquals("", map["{{NGAY_KIEM_TRA}}"])
        assertEquals("", map["{{can_bo_giam_sat}}"])
        assertEquals("", map["{{CAN_BO_GIAM_SAT}}"])
    }

    @Test
    fun testMapToPlaceholdersEnglishSchemaFormatsDefaultsToEmpty() {
        // 1. English format with summary_fields as JSONArray and manual_fields as JSONArray
        val enArrayTemplate = TemplateItemEntity(
            id = "en-array-template",
            name = "Daily Site Report",
            description = "English template array",
            systemPrompt = "Prompt",
            schemaJson = """
                {
                    "name": "Daily Site Report",
                    "summary_fields": [
                        { "name": "work_completed", "type": "list" },
                        { "name": "safety_notes", "type": "list", "placeholder": "{{SAFETY_NOTES}}" }
                    ],
                    "manual_fields": ["inspector_name", "site_location"]
                }
            """.trimIndent()
        )

        val log1 = DailyLogEntity(date = "2026-10-24")
        val map1 = PlaceholderMapper.mapToPlaceholders(log1, enArrayTemplate)

        assertEquals("", map1["{{work_completed}}"])
        assertEquals("", map1["{{WORK_COMPLETED}}"])
        assertEquals("", map1["{{safety_notes}}"])
        assertEquals("", map1["{{SAFETY_NOTES}}"])
        assertEquals("", map1["{{ SAFETY_NOTES }}"])
        assertEquals("", map1["{{inspector_name}}"])
        assertEquals("", map1["{{INSPECTOR_NAME}}"])
        assertEquals("", map1["{{site_location}}"])
        assertEquals("", map1["{{SITE_LOCATION}}"])

        // 2. English format with summary_fields as JSONObject and manual_fields as JSONObject
        val enObjectTemplate = TemplateItemEntity(
            id = "en-object-template",
            name = "Engineering Log",
            description = "English template object",
            systemPrompt = "Prompt",
            schemaJson = """
                {
                    "name": "Engineering Log",
                    "summary_fields": {
                        "structural_check": { "type": "string" },
                        "mep_progress": { "type": "string", "placeholder": "{{MEP_PROGRESS}}" }
                    },
                    "manual_fields": {
                        "lead_engineer": { "type": "string" }
                    }
                }
            """.trimIndent()
        )

        val log2 = DailyLogEntity(date = "2026-10-24")
        val map2 = PlaceholderMapper.mapToPlaceholders(log2, enObjectTemplate)

        assertEquals("", map2["{{structural_check}}"])
        assertEquals("", map2["{{STRUCTURAL_CHECK}}"])
        assertEquals("", map2["{{mep_progress}}"])
        assertEquals("", map2["{{MEP_PROGRESS}}"])
        assertEquals("", map2["{{ MEP_PROGRESS }}"])
        assertEquals("", map2["{{lead_engineer}}"])
        assertEquals("", map2["{{LEAD_ENGINEER}}"])
    }

    @Test
    fun testMapToPlaceholdersPreservesFilledDataWhileDefaultingUnfilled() {
        val template = TemplateItemEntity(
            id = "custom_mixed",
            name = "Mẫu hỗn hợp",
            description = "Test mixed",
            systemPrompt = "Prompt",
            schemaJson = """
                {
                    "truong_tong_hop": {
                        "cong_viec_thi_cong": { "kieu": "danh_sach" },
                        "chua_co_du_lieu": { "kieu": "van_ban" }
                    }
                }
            """.trimIndent()
        )

        val reportContent = DailyReportContent(
            congViecThiCong = listOf("Đổ bê tông móng M1")
        )
        val log = DailyLogEntity(
            date = "2026-10-24",
            reportContent = reportContent
        )

        val map = PlaceholderMapper.mapToPlaceholders(log, template)

        assertEquals("- Đổ bê tông móng M1", map["{{CONG_VIEC_THI_CONG}}"])
        assertEquals("- Đổ bê tông móng M1", map["{{cong_viec_thi_cong}}"])
        assertEquals("", map["{{chua_co_du_lieu}}"])
        assertEquals("", map["{{CHUA_CO_DU_LIEU}}"])
    }

    @Test
    fun testEquipmentFallbackFromNoteContentWhenDailyInputBlank() {
        val reportContent = DailyReportContent(
            allFields = mapOf(
                "may_moc" to "2 máy lu, 1 máy đào"
            )
        )
        val log = DailyLogEntity(
            date = "2026-10-25",
            dailyInput = null,
            reportContent = reportContent
        )

        val map = PlaceholderMapper.buildPlaceholderMap(log.date, log.dailyInput, log.reportContent)

        assertEquals("2 máy lu, 1 máy đào", map["{{MAY_MOC}}"])
        assertEquals("2 máy lu, 1 máy đào", map["{{may_moc}}"])
        assertEquals("2 máy lu, 1 máy đào", map["{{THIET_BI}}"])
        assertEquals("2 máy lu, 1 máy đào", map["{{thiet_bi}}"])
    }

    @Test
    fun testEquipmentPrefersDailyInputOverNoteContent() {
        val dailyInput = DailyInputEntity(
            date = "2026-10-25",
            equipmentList = "1 cẩu tháp, 2 máy trộn"
        )
        val reportContent = DailyReportContent(
            allFields = mapOf(
                "may_moc" to "máy hàn dự phòng"
            )
        )
        val log = DailyLogEntity(
            date = "2026-10-25",
            dailyInput = dailyInput,
            reportContent = reportContent
        )

        val map = PlaceholderMapper.buildPlaceholderMap(log.date, log.dailyInput, log.reportContent)

        assertEquals("1 cẩu tháp, 2 máy trộn", map["{{MAY_MOC}}"])
        assertEquals("1 cẩu tháp, 2 máy trộn", map["{{THIET_BI}}"])
    }
}
