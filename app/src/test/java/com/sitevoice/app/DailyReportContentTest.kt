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
}
