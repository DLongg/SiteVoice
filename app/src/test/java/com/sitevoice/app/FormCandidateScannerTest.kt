package com.sitevoice.app

import com.sitevoice.app.formsetup.FormCandidateScanner
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

        assertTrue("Cần tìm thấy các ứng viên", candidates.size >= 5)

        val dateCandidates = candidates.filter { it.loaiGoiY == "ngay" }
        assertEquals(2, dateCandidates.size)

        val checkboxCandidates = candidates.filter { it.tinHieu == "o_tich" }
        assertEquals(2, checkboxCandidates.size)

        val emptyBlockCandidates = candidates.filter { it.tinHieu == "doan_trong" }
        assertEquals(2, emptyBlockCandidates.size)
        val thiCongCandidate = emptyBlockCandidates.find { it.tieuDeGanNhat.contains("thi công", ignoreCase = true) }
        assertTrue(thiCongCandidate != null)
        assertEquals(3, thiCongCandidate?.soDongLienTiep)
        assertEquals(166, thiCongCandidate?.startIndex)
        assertEquals(169, thiCongCandidate?.endIndex)
    }

    @Test
    fun testHeadingPatternRecognition() {
        // Số La Mã
        assertTrue(FormCandidateScanner.isHeading("I. THÔNG TIN CHUNG"))
        assertTrue(FormCandidateScanner.isHeading("II. TÌNH HÌNH THI CÔNG:"))
        assertTrue(FormCandidateScanner.isHeading("III) NỘI DUNG THI CÔNG"))
        assertTrue(FormCandidateScanner.isHeading("IV. CÔNG TÁC NGHIỆM THU"))
        assertTrue(FormCandidateScanner.isHeading("X. PHỤ LỤC HỒ SƠ"))

        // Chữ cái
        assertTrue(FormCandidateScanner.isHeading("A. CHỦ ĐẦU TƯ"))
        assertTrue(FormCandidateScanner.isHeading("B. TƯ VẤN GIÁM SÁT:"))
        assertTrue(FormCandidateScanner.isHeading("a. Đơn vị thi công xây dựng"))
        assertTrue(FormCandidateScanner.isHeading("b) Danh mục thiết bị"))

        // Số và mục con phân cấp
        assertTrue(FormCandidateScanner.isHeading("1. Thời tiết:"))
        assertTrue(FormCandidateScanner.isHeading("2. Lực lượng thi công"))
        assertTrue(FormCandidateScanner.isHeading("3) Vật tư chính"))
        assertTrue(FormCandidateScanner.isHeading("1.1 Ban chỉ huy:"))
        assertTrue(FormCandidateScanner.isHeading("3.1 Công việc thi công trong ngày:"))
        assertTrue(FormCandidateScanner.isHeading("1.2.3 Kiểm tra an toàn lao động"))

        // Dòng kết thúc bằng dấu : lên tới 150 ký tự
        assertTrue(FormCandidateScanner.isHeading("5. Công tác an toàn lao động và vệ sinh môi trường trên toàn bộ công trường:"))
        assertTrue(FormCandidateScanner.isHeading("Chủ đầu tư:"))
        assertTrue(FormCandidateScanner.isHeading("Ý kiến chỉ đạo của Tư vấn giám sát tại hiện trường:"))

        // Phủ định: Không phải heading
        assertFalse(FormCandidateScanner.isHeading("................................"))
        assertFalse(FormCandidateScanner.isHeading("________________________________"))
        assertFalse(FormCandidateScanner.isHeading(""))
        assertFalse(FormCandidateScanner.isHeading("   "))
        assertFalse(FormCandidateScanner.isHeading("Hà Nội, ngày ..... tháng ..... năm 2026\n"))
        assertFalse(FormCandidateScanner.isHeading("- Ban chỉ huy công trường: ..... người"))
    }

    @Test
    fun testTableScanningPreservesLeftLabelForEmptyAndDottedCells() {
        val tableElement = createTableElement(
            listOf(
                // Row 0: Ô trái là nhãn "Chủ đầu tư:", Ô phải rỗng
                listOf(
                    listOf(Pair(10, "Chủ đầu tư:\n")),
                    listOf(Pair(25, "\n"))
                ),
                // Row 1: Ô trái là "Ban chỉ huy:", Ô phải chứa chấm "..... người"
                listOf(
                    listOf(Pair(30, "Ban chỉ huy:\n")),
                    listOf(Pair(50, "..... người\n"))
                ),
                // Row 2: Ô trái là "3. Công việc thi công:", Ô phải chứa 3 dòng chấm liên tiếp
                listOf(
                    listOf(Pair(70, "3. Công việc thi công:\n")),
                    listOf(
                        Pair(100, "................................\n"),
                        Pair(140, "................................\n"),
                        Pair(180, "................................\n")
                    )
                )
            )
        )

        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(tableElement)
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(3, candidates.size)

        // Candidate 1: Ô trống bên phải kế thừa nhãn "Chủ đầu tư:"
        val c0 = candidates[0]
        assertEquals("o_bang", c0.tinHieu)
        assertEquals("Chủ đầu tư:", c0.truoc)
        assertEquals("Chủ đầu tư:", c0.tieuDeGanNhat)
        assertEquals("van_ban", c0.loaiGoiY)
        assertEquals(1, c0.soDongLienTiep)

        // Candidate 2: Ô chấm bên phải kế thừa nhãn "Ban chỉ huy:", nhận diện loại số
        val c1 = candidates[1]
        assertEquals("o_bang", c1.tinHieu)
        assertEquals("Ban chỉ huy:", c1.truoc)
        assertEquals("người", c1.sau)
        assertEquals("Ban chỉ huy:", c1.tieuDeGanNhat)
        assertEquals("so", c1.loaiGoiY)

        // Candidate 3: 3 dòng chấm liên tiếp gộp thành 1 chỗ điền danh_sach
        val c2 = candidates[2]
        assertEquals("o_bang", c2.tinHieu)
        assertEquals("3. Công việc thi công:", c2.truoc)
        assertEquals("3. Công việc thi công:", c2.tieuDeGanNhat)
        assertEquals(3, c2.soDongLienTiep)
        assertEquals("danh_sach", c2.loaiGoiY)
        assertEquals(100, c2.startIndex)
        assertEquals(213, c2.endIndex) // 180 + "................................\n".length = 180 + 33 = 213
    }

    @Test
    fun testConsecutiveDotLinesMergingUnderHeading() {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    // Đề mục
                    put(createParagraphElement(1, 35, "3. Công việc thi công trong ngày:\n"))
                    // 4 dòng chấm liên tiếp
                    put(createParagraphElement(36, 70, "................................\n"))
                    put(createParagraphElement(71, 105, "................................\n"))
                    put(createParagraphElement(106, 140, "................................\n"))
                    put(createParagraphElement(141, 175, "................................\n"))
                    // Đề mục tiếp theo
                    put(createParagraphElement(176, 210, "4. Công việc nghiệm thu trong ngày:\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(1, candidates.size)

        val merged = candidates[0]
        assertEquals("dong_cham", merged.tinHieu)
        assertEquals(4, merged.soDongLienTiep)
        assertEquals("danh_sach", merged.loaiGoiY)
        assertEquals(36, merged.startIndex)
        assertEquals(175, merged.endIndex)
        assertTrue(merged.tieuDeGanNhat.contains("thi công"))
    }

    @Test
    fun testSingleDotLineUnderHeadingProducesVanBan() {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 20, "2. Ghi chú chung:\n"))
                    put(createParagraphElement(21, 55, "................................\n"))
                    put(createParagraphElement(56, 80, "3. Đề mục tiếp theo:\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(1, candidates.size)

        val single = candidates[0]
        assertEquals("dong_cham", single.tinHieu)
        assertEquals(1, single.soDongLienTiep)
        assertEquals("van_ban", single.loaiGoiY)
        assertEquals(21, single.startIndex)
        assertEquals(55, single.endIndex)
    }

    @Test
    fun testInlineDotsMaintainSeparation() {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 40, "Hà Nội, ngày ..... tháng ..... năm 2026\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        // Phải tách thành 2 ứng viên riêng biệt (ngày và tháng), không được gộp thành danh_sach
        assertEquals(2, candidates.size)
        assertEquals("ngay", candidates[0].loaiGoiY)
        assertEquals(1, candidates[0].soDongLienTiep)
        assertTrue(candidates[0].truoc.contains("ngày"))

        assertEquals("ngay", candidates[1].loaiGoiY)
        assertEquals(1, candidates[1].soDongLienTiep)
        assertTrue(candidates[1].truoc.contains("tháng"))
    }

    @Test
    fun testStandardConstructionLogWithAll6SectionsNoBleed() = runBlocking {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    var idx = 1
                    fun addP(text: String): JSONObject {
                        val start = idx
                        val end = idx + text.length
                        idx = end
                        return createParagraphElement(start, end, text)
                    }

                    put(addP("1. Tình hình thời tiết:\n"))
                    put(addP("- Sáng: trời nắng ☐ trời mưa ☑\n"))
                    put(addP("2. Lực lượng thi công\n"))
                    put(addP("- Ban chỉ huy: ..... người\n"))
                    put(addP("- Công nhân: ..... người\n"))
                    put(addP("* Máy móc thiết bị thi công chính: ....................\n"))
                    put(addP("3. Công việc thi công trong ngày:\n"))
                    put(addP("....................................................\n"))
                    put(addP("....................................................\n"))
                    put(addP("4. Công việc nghiệm thu trong ngày:\n"))
                    put(addP("....................................................\n"))
                    put(addP("5. An toàn lao động và vệ sinh môi trường:\n"))
                    put(addP("....................................................\n"))
                    put(addP("6. Ý kiến của các đơn vị:\n"))
                    put(addP("a. Chủ đầu tư: ....................\n"))
                    put(addP("b. Tư vấn giám sát: ....................\n"))
                    put(addP("c. Nhà thầu thi công xây dựng: ....................\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertTrue(candidates.size >= 8)

        val describer = com.sitevoice.app.formsetup.FormLlmDescriber()
        val described = describer.describeCandidates(candidates)

        val mayMoc = described.find { it.ma.contains("MAY_MOC") }
        assertNotNull("Máy móc phải được nhận diện", mayMoc)
        assertEquals("MAY_MOC", mayMoc?.ma)
        assertEquals("nhap_tay", mayMoc?.nguon)
        assertFalse(mayMoc?.tieuDeGanNhat?.contains("Công việc thi công trong ngày") == true)

        val thiCong = described.find { it.ma.contains("CONG_VIEC_THI_CONG") }
        assertNotNull("Công việc thi công phải được nhận diện", thiCong)
        assertEquals("CONG_VIEC_THI_CONG", thiCong?.ma)
        assertEquals("thi_cong", thiCong?.nguon)

        val ykCdt = described.find { it.ma == "Y_KIEN_CDT" }
        assertNotNull("Ý kiến CĐT phải được nhận diện", ykCdt)
        assertEquals("y_kien_cdt", ykCdt?.nguon)
        assertTrue("Heading phải là Chủ đầu tư, không bị tràn từ mục 3", ykCdt?.tieuDeGanNhat?.contains("Chủ đầu tư") == true)

        val ykTvgs = described.find { it.ma == "Y_KIEN_TVGS" }
        assertNotNull("Ý kiến TVGS phải được nhận diện", ykTvgs)
        assertEquals("y_kien_tvgs", ykTvgs?.nguon)
        assertTrue("Heading phải là Tư vấn giám sát", ykTvgs?.tieuDeGanNhat?.contains("Tư vấn giám sát") == true)

        val ykNhaThau = described.find { it.ma == "Y_KIEN_NHA_THAU" }
        assertNotNull("Ý kiến Nhà thầu phải được nhận diện (không bị gán nhầm sang CONG_VIEC_THI_CONG)", ykNhaThau)
        assertEquals("y_kien_nha_thau", ykNhaThau?.nguon)
        assertTrue("Heading phải là Nhà thầu", ykNhaThau?.tieuDeGanNhat?.contains("Nhà thầu") == true)
    }

    @Test
    fun testUserCustomPlaceholderTagsRecognized() {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 40, "Ý kiến Chủ đầu tư: {{Y_KIEN_CDT}}\n"))
                    put(createParagraphElement(41, 80, "Thẻ tùy chỉnh: {{CUSTOM_MY_TAG}}\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(2, candidates.size)

        val c1 = candidates[0]
        assertEquals("the_ma", c1.tinHieu)
        assertEquals("Y_KIEN_CDT", c1.ma)
        assertEquals("Y_KIEN_CDT", c1.originalMa)
        assertEquals(1.0f, c1.doTinCay, 0.001f)

        val c2 = candidates[1]
        assertEquals("the_ma", c2.tinHieu)
        assertEquals("CUSTOM_MY_TAG", c2.ma)
        assertEquals("CUSTOM_MY_TAG", c2.originalMa)
    }

    @Test
    fun testTableMultiParagraphLabelAndDotsSeparation() {
        val tableElement = createTableElement(
            listOf(
                listOf(
                    listOf(
                        Pair(10, "Ý kiến Chủ đầu tư:\n"),
                        Pair(35, "................................\n")
                    )
                )
            )
        )

        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(tableElement)
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(1, candidates.size)
        val c = candidates[0]
        assertEquals("Ý kiến Chủ đầu tư:", c.tieuDeGanNhat)
        assertEquals("Ý kiến Chủ đầu tư:", c.truoc)
        assertEquals("o_bang", c.tinHieu)
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

    private fun createTableElement(rows: List<List<List<Pair<Int, String>>>>): JSONObject {
        val tableObj = JSONObject()
        val tableRowsArray = JSONArray()

        for (rowCells in rows) {
            val rowObj = JSONObject()
            val cellsArray = JSONArray()

            for (cellParagraphs in rowCells) {
                val cellObj = JSONObject()
                val contentArray = JSONArray()

                for ((start, text) in cellParagraphs) {
                    val end = start + text.length
                    contentArray.put(createParagraphElement(start, end, text))
                }
                cellObj.put("content", contentArray)
                cellsArray.put(cellObj)
            }
            rowObj.put("tableCells", cellsArray)
            tableRowsArray.put(rowObj)
        }

        tableObj.put("tableRows", tableRowsArray)
        return JSONObject().apply { put("table", tableObj) }
    }
}
