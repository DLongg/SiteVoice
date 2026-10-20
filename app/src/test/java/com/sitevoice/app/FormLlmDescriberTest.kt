package com.sitevoice.app

import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormLlmDescriber
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormLlmDescriberTest {

    @Test
    fun testDeterministicRulesIdentifyAllStandardFields() = runBlocking {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 15,
                truoc = "Hà Nội, ngày",
                sau = "tháng",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "",
                loaiGoiY = "ngay"
            ),
            FormCandidate(
                id = 2,
                startIndex = 20,
                endIndex = 25,
                truoc = "tháng",
                sau = "năm 2026",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "",
                loaiGoiY = "ngay"
            ),
            FormCandidate(
                id = 3,
                startIndex = 60,
                endIndex = 61,
                truoc = "- Sáng: trời nắng",
                sau = "trời mưa",
                tinHieu = "o_tich",
                tieuDeGanNhat = "1. Thời tiết:",
                loaiGoiY = "o_tich"
            ),
            FormCandidate(
                id = 4,
                startIndex = 70,
                endIndex = 71,
                truoc = "- Sáng: trời mưa",
                sau = "- Chiều:",
                tinHieu = "o_tich",
                tieuDeGanNhat = "1. Thời tiết:",
                loaiGoiY = "o_tich"
            ),
            FormCandidate(
                id = 5,
                startIndex = 100,
                endIndex = 105,
                truoc = "- Ban chỉ huy công trường:",
                sau = "người",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Lực lượng thi công:",
                loaiGoiY = "so"
            ),
            FormCandidate(
                id = 6,
                startIndex = 120,
                endIndex = 125,
                truoc = "- Công nhân:",
                sau = "người",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Lực lượng thi công:",
                loaiGoiY = "so"
            ),
            FormCandidate(
                id = 7,
                startIndex = 150,
                endIndex = 155,
                truoc = "* Máy móc – thiết bị thi công chính:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Lực lượng thi công:",
                loaiGoiY = "van_ban"
            ),
            FormCandidate(
                id = 8,
                startIndex = 200,
                endIndex = 220,
                truoc = "3. Công việc thi công trong ngày:",
                sau = "4. Công việc nghiệm thu trong ngày:",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "3. Công việc thi công trong ngày:",
                soDongLienTiep = 5,
                loaiGoiY = "danh_sach"
            ),
            FormCandidate(
                id = 9,
                startIndex = 250,
                endIndex = 270,
                truoc = "4. Công việc nghiệm thu trong ngày:",
                sau = "5. An toàn lao động...",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "4. Công việc nghiệm thu trong ngày:",
                soDongLienTiep = 3,
                loaiGoiY = "danh_sach"
            ),
            FormCandidate(
                id = 10,
                startIndex = 300,
                endIndex = 320,
                truoc = "5. An toàn lao động và vệ sinh môi trường:",
                sau = "6. Ý kiến của các đơn vị:",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "5. An toàn lao động và vệ sinh môi trường:",
                soDongLienTiep = 2,
                loaiGoiY = "danh_sach"
            )
        )

        val describer = FormLlmDescriber()
        val result = describer.describeCandidates(candidates)

        assertEquals("NGAY", result[0].ma)
        assertEquals("THANG", result[1].ma)
        assertEquals("SANG_NANG", result[2].ma)
        assertEquals("SANG_MUA", result[3].ma)
        assertEquals("BAN_CHI_HUY", result[4].ma)
        assertEquals("CONG_NHAN", result[5].ma)
        assertEquals("MAY_MOC", result[6].ma)
        assertEquals("CONG_VIEC_THI_CONG", result[7].ma)
        assertEquals("thi_cong", result[7].nguon)
        assertEquals("NGHIEM_THU", result[8].ma)
        assertEquals("nghiem_thu", result[8].nguon)
        assertEquals("AN_TOAN", result[9].ma)
        assertEquals("an_toan", result[9].nguon)

        // Không trường nào bị bỏ qua
        assertTrue(result.all { !it.boQua })
    }
}
