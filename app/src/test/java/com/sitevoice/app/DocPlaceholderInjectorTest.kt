package com.sitevoice.app

import com.sitevoice.app.formsetup.DocPlaceholderInjector
import com.sitevoice.app.formsetup.FormCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocPlaceholderInjectorTest {

    @Test
    fun testBuildBatchUpdatePayloadOrdersDescending() {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 15,
                truoc = "",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "",
                loaiGoiY = "ngay",
                ma = "NGAY"
            ),
            FormCandidate(
                id = 2,
                startIndex = 50,
                endIndex = 51,
                truoc = "",
                sau = "",
                tinHieu = "o_tich",
                tieuDeGanNhat = "",
                loaiGoiY = "o_tich",
                ma = "SANG_NANG"
            ),
            FormCandidate(
                id = 3,
                startIndex = 100,
                endIndex = 120,
                truoc = "",
                sau = "",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "",
                loaiGoiY = "danh_sach",
                ma = "CONG_VIEC_THI_CONG"
            ),
            FormCandidate(
                id = 4,
                startIndex = 200,
                endIndex = 250,
                truoc = "",
                sau = "",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "",
                loaiGoiY = "danh_sach",
                ma = "CHU_KY",
                boQua = true // Khối chữ ký bị bỏ qua
            )
        )

        val payload = DocPlaceholderInjector.buildBatchUpdatePayload(candidates)
        val requests = payload.getJSONArray("requests")

        // Ứng viên 3 (doan_trong) chỉ insert (1 request)
        // Ứng viên 2 (o_tich): delete + insert (2 requests)
        // Ứng viên 1 (dong_cham): delete + insert (2 requests)
        // Tổng: 5 requests
        assertEquals(5, requests.length())

        // Request đầu tiên phải thao tác trên vị trí lớn nhất (100) và là insertText (không delete range doan_trong)
        val firstInsert = requests.getJSONObject(0).getJSONObject("insertText")
        assertEquals(100, firstInsert.getJSONObject("location").getInt("index"))
        assertEquals("{{CONG_VIEC_THI_CONG}}", firstInsert.getString("text"))

        // Request tiếp theo là vị trí 50 (delete + insert)
        val secondDelete = requests.getJSONObject(1).getJSONObject("deleteContentRange")
        assertEquals(50, secondDelete.getJSONObject("range").getInt("startIndex"))

        val secondInsert = requests.getJSONObject(2).getJSONObject("insertText")
        assertEquals(50, secondInsert.getJSONObject("location").getInt("index"))

        // Request cuối cùng là vị trí 10 (delete + insert)
        val lastDelete = requests.getJSONObject(3).getJSONObject("deleteContentRange")
        assertEquals(10, lastDelete.getJSONObject("range").getInt("startIndex"))
    }

    @Test
    fun testTheMaUnchangedGeneratesZeroRequests() {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 100,
                endIndex = 115,
                truoc = "Ý kiến:",
                sau = "",
                tinHieu = "the_ma",
                tieuDeGanNhat = "Chủ đầu tư",
                loaiGoiY = "van_ban",
                ma = "Y_KIEN_CDT",
                originalMa = "Y_KIEN_CDT"
            )
        )

        val payload = DocPlaceholderInjector.buildBatchUpdatePayload(candidates)
        val requests = payload.getJSONArray("requests")
        assertEquals(0, requests.length())
    }

    @Test
    fun testTheMaChangedGeneratesDeleteAndInsert() {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 100,
                endIndex = 115,
                truoc = "Ý kiến:",
                sau = "",
                tinHieu = "the_ma",
                tieuDeGanNhat = "Chủ đầu tư",
                loaiGoiY = "van_ban",
                ma = "Y_KIEN_CHU_DAU_TU",
                originalMa = "Y_KIEN_CDT"
            )
        )

        val payload = DocPlaceholderInjector.buildBatchUpdatePayload(candidates)
        val requests = payload.getJSONArray("requests")
        assertEquals(2, requests.length())

        val del = requests.getJSONObject(0).getJSONObject("deleteContentRange").getJSONObject("range")
        assertEquals(100, del.getInt("startIndex"))
        assertEquals(115, del.getInt("endIndex"))

        val ins = requests.getJSONObject(1).getJSONObject("insertText")
        assertEquals(100, ins.getJSONObject("location").getInt("index"))
        assertEquals("{{Y_KIEN_CHU_DAU_TU}}", ins.getString("text"))
    }
}
