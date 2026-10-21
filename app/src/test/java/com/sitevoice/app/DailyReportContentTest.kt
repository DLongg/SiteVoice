package com.sitevoice.app

import com.sitevoice.app.template.DailyReportContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyReportContentTest {

    @Test
    fun testParseCompleteJson() {
        val json = """
            {
              "cong_viec_thi_cong": [
                "Đổ bê tông sàn tầng 3 trục A-C",
                "Gia công cốt thép dầm D1-D4"
              ],
              "nghiem_thu": [
                "Nghiệm thu cốp pha sàn tầng 3 lúc 14h30"
              ],
              "an_toan": [
                "Trang bị đầy đủ mũ và dây an toàn trên cao"
              ],
              "y_kien_cdt": "Đẩy nhanh tiến độ hoàn thiện tầng 2",
              "y_kien_tvgs": "Lưu ý bảo dưỡng bê tông sau đổ 4 giờ",
              "y_kien_nha_thau": "Đề nghị cung cấp bản vẽ MEP điều chỉnh",
              "chua_phan_loai": [
                "Kiểm tra vật tư cát đá tại bãi tập kết"
              ]
            }
        """.trimIndent()

        val content = DailyReportContent.fromJson(json)

        assertEquals(2, content.congViecThiCong.size)
        assertEquals("Đổ bê tông sàn tầng 3 trục A-C", content.congViecThiCong[0])
        assertEquals(1, content.nghiemThu.size)
        assertEquals(1, content.anToan.size)
        assertEquals("Đẩy nhanh tiến độ hoàn thiện tầng 2", content.yKienCdt)
        assertEquals("Lưu ý bảo dưỡng bê tông sau đổ 4 giờ", content.yKienTvgs)
        assertEquals("Đề nghị cung cấp bản vẽ MEP điều chỉnh", content.yKienNhaThau)
        assertEquals(1, content.chuaPhanLoai.size)
    }

    @Test
    fun testParseEmptyOrMalformedJson() {
        val emptyContent = DailyReportContent.fromJson("")
        assertTrue(emptyContent.congViecThiCong.isEmpty())
        assertTrue(emptyContent.yKienCdt.isEmpty())

        val malformedContent = DailyReportContent.fromJson("{invalid json")
        assertTrue(malformedContent.congViecThiCong.isEmpty())
        assertTrue(malformedContent.yKienCdt.isEmpty())
    }

    @Test
    fun testFlattenNestedWeatherAndManpower() {
        val json = """
            {
              "thoi_tiet": {
                "s_n": true,
                "s_m": false,
                "c_n": false,
                "c_m": true,
                "t_bt": true,
                "t_m": false,
                "tong_quan_thoi_tiet": "Sáng trời nắng, chiều trời mưa, tối bình thường."
              },
              "nhan_luc": {
                "ng_ch": "8",
                "ng_cn": "10"
              },
              "may_moc": [
                "- Máy cắt",
                "- Máy đục"
              ],
              "cong_viec_thi_cong": [
                "3.1 Điện",
                "\t- Thi công hệ thống điện tại tầng 8.",
                "\t- Thi công hệ thống điện tại tầng 5.",
                "3.2 Nước",
                "\t- Thi công trục thoát nước số 11."
              ]
            }
        """.trimIndent()

        val content = DailyReportContent.fromJson(json)

        assertEquals(true, content.allFields["s_n"])
        assertEquals(false, content.allFields["s_m"])
        assertEquals(false, content.allFields["c_n"])
        assertEquals(true, content.allFields["c_m"])
        assertEquals(true, content.allFields["t_bt"])
        assertEquals(false, content.allFields["t_m"])
        assertEquals("Sáng trời nắng, chiều trời mưa, tối bình thường.", content.allFields["tong_quan_thoi_tiet"])
        assertEquals("8", content.allFields["ng_ch"])
        assertEquals("10", content.allFields["ng_cn"])

        val mayMoc = content.allFields["may_moc"] as List<*>
        assertEquals(2, mayMoc.size)

        assertEquals(5, content.congViecThiCong.size)
        assertEquals("3.1 Điện", content.congViecThiCong[0])
    }

    @Test
    fun testParseMultilineStringFields() {
        val json = """
            {
              "cong_viec_thi_cong": "3.1 Điện\n\t- Thi công hệ thống điện tại tầng 8.\n3.2 Nước\n\t- Thi công trục thoát nước 11.",
              "nghiem_thu": "- Nghiệm thu cốp pha sàn",
              "an_toan": "- Vệ sinh mặt sàn",
              "y_kien_cdt": "Đẩy nhanh tiến độ"
            }
        """.trimIndent()

        val content = DailyReportContent.fromJson(json)

        assertEquals(4, content.congViecThiCong.size)
        assertEquals("3.1 Điện", content.congViecThiCong[0])
        assertEquals("\t- Thi công hệ thống điện tại tầng 8.", content.congViecThiCong[1])
        assertEquals("3.2 Nước", content.congViecThiCong[2])
        assertEquals("\t- Thi công trục thoát nước 11.", content.congViecThiCong[3])

        assertEquals(1, content.nghiemThu.size)
        assertEquals("- Nghiệm thu cốp pha sàn", content.nghiemThu[0])
        assertEquals(1, content.anToan.size)
        assertEquals("Đẩy nhanh tiến độ", content.yKienCdt)
    }
}
