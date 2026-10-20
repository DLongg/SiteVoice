package com.sitevoice.app

import com.sitevoice.app.formsetup.FormCandidateScanner
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormCandidateScannerTest {

    @Test
    fun testScanDocumentFindsDotsCheckboxesAndEmptyBlocks() {
        val sampleDoc = JSONObject().apply {
            put("documentId", "sample_doc_123")
            put("body", JSONObject().apply {
                val contentArray = JSONArray().apply {
                    // 1. Ngày tháng có chấm: "Hà Nội, ngày ..... tháng ..... năm 2026\n" (startIndex: 1, endIndex: 40)
                    put(createParagraphElement(1, 40, "Hà Nội, ngày ..... tháng ..... năm 2026\n"))

                    // 2. Tiêu đề thời tiết: "1. Thời tiết:\n" (startIndex: 41, endIndex: 56)
                    put(createParagraphElement(41, 56, "1. Thời tiết:\n"))

                    // 3. Ô tích thời tiết: "- Sáng: trời nắng ☐ trời mưa ☑\n" (startIndex: 57, endIndex: 89)
                    put(createParagraphElement(57, 89, "- Sáng: trời nắng ☐ trời mưa ☑\n"))

                    // 4. Nhân lực: "- Ban chỉ huy công trường: ..... người\n" (startIndex: 90, endIndex: 130)
                    put(createParagraphElement(90, 130, "- Ban chỉ huy công trường: ..... người\n"))

                    // 5. Tiêu đề thi công: "3. Công việc thi công trong ngày:\n" (startIndex: 131, endIndex: 165)
                    put(createParagraphElement(131, 165, "3. Công việc thi công trong ngày:\n"))

                    // 6. Nhiều dòng trống liên tiếp dưới mục thi công
                    put(createParagraphElement(166, 167, "\n"))
                    put(createParagraphElement(167, 168, "\n"))
                    put(createParagraphElement(168, 169, "\n"))

                    // 7. Tiêu đề tiếp theo: "4. Công việc nghiệm thu trong ngày:\n" (startIndex: 170, endIndex: 206)
                    put(createParagraphElement(170, 206, "4. Công việc nghiệm thu trong ngày:\n"))

                    // 8. Dòng trống dưới nghiệm thu
                    put(createParagraphElement(207, 208, "\n"))
                    put(createParagraphElement(208, 209, "\n"))
                }
                put("content", contentArray)
            })
        }

        val candidates = FormCandidateScanner.scanDocument(sampleDoc)

        // Phải phát hiện:
        // - 2 chỗ chấm ngày/tháng
        // - 2 ô tích thời tiết (☐ và ☑)
        // - 1 chỗ chấm nhân lực
        // - 1 khối đoạn trống gộp dưới mục 3 thi công
        // - 1 khối đoạn trống gộp dưới mục 4 nghiệm thu
        assertTrue("Cần tìm thấy các ứng viên", candidates.size >= 5)

        val dateCandidates = candidates.filter { it.loaiGoiY == "ngay" }
        assertEquals(2, dateCandidates.size)

        val checkboxCandidates = candidates.filter { it.tinHieu == "o_tich" }
        assertEquals(2, checkboxCandidates.size)

        val emptyBlockCandidates = candidates.filter { it.tinHieu == "doan_trong" }
        assertEquals(2, emptyBlockCandidates.size)
        // Đoạn trống dưới mục 3 phải được gộp từ 3 dòng trống liên tiếp
        val thiCongCandidate = emptyBlockCandidates.find { it.tieuDeGanNhat.contains("thi công", ignoreCase = true) }
        assertTrue(thiCongCandidate != null)
        assertEquals(3, thiCongCandidate?.soDongLienTiep)
        assertEquals(166, thiCongCandidate?.startIndex)
        assertEquals(169, thiCongCandidate?.endIndex)
    }

    private fun createParagraphElement(start: Int, end: Int, text: String): JSONObject {
        return JSONObject().apply {
            put("startIndex", start)
            put("endIndex", end)
            put("paragraph", JSONObject().apply {
                put("elements", JSONArray().apply {
                    put(JSONObject().apply {
                        put("startIndex", start)
                        put("endIndex", end)
                        put("textRun", JSONObject().apply {
                            put("content", text)
                        })
                    })
                })
            })
        }
    }
}
