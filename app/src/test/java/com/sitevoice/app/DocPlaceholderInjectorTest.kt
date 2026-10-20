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

        // 3 ứng viên hợp lệ x 2 thao tác (delete + insert) = 6 requests
        assertEquals(6, requests.length())

        // Request đầu tiên phải thao tác trên vị trí lớn nhất (100)
        val firstDelete = requests.getJSONObject(0).getJSONObject("deleteContentRange")
        val range = firstDelete.getJSONObject("range")
        assertEquals(100, range.getInt("startIndex"))
        assertEquals(120, range.getInt("endIndex"))

        val firstInsert = requests.getJSONObject(1).getJSONObject("insertText")
        assertEquals(100, firstInsert.getJSONObject("location").getInt("index"))
        assertEquals("{{CONG_VIEC_THI_CONG}}", firstInsert.getString("text"))

        // Request tiếp theo phải là vị trí 50
        val secondDelete = requests.getJSONObject(2).getJSONObject("deleteContentRange")
        assertEquals(50, secondDelete.getJSONObject("range").getInt("startIndex"))

        // Request cuối cùng phải là vị trí 10
        val lastDelete = requests.getJSONObject(4).getJSONObject("deleteContentRange")
        assertEquals(10, lastDelete.getJSONObject("range").getInt("startIndex"))
    }
}
