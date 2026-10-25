package com.sitevoice.app

import com.sitevoice.app.formsetup.DocPlaceholderInjector
import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormCandidateScanner
import com.sitevoice.app.formsetup.FormLlmDescriber
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Adversarial and Empirical Stress Tests for Milestone M1
 * Authored and executed by teamwork_preview_challenger (challenger_m1_1).
 */
class ChallengerM1StressTest {

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

    // =========================================================================
    // QUESTION 1: Does "a. Chủ đầu tư: ............" detect heading & dots?
    // =========================================================================

    @Test
    fun testQuestion1_PrefixHeadingWithDots() {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 40, "a. Chủ đầu tư: ....................\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(1, candidates.size)
        val c = candidates[0]

        // Must detect heading "a. Chủ đầu tư:"
        assertEquals("a. Chủ đầu tư:", c.tieuDeGanNhat)
        assertEquals("a. Chủ đầu tư:", c.truoc)
        assertEquals("dong_cham", c.tinHieu)
        assertEquals(1, c.soDongLienTiep)
        assertEquals("van_ban", c.loaiGoiY)
        assertEquals(16, c.startIndex) // "a. Chủ đầu tư: ".length = 15 -> index 1 + 15 = 16
    }

    @Test
    fun testQuestion1_PrefixHeadingWithMultiLineDots_MergesCorrectly() {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 40, "a. Chủ đầu tư: ....................\n"))
                    put(createParagraphElement(41, 75, "..................................\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(1, candidates.size)
        val c = candidates[0]

        assertEquals("a. Chủ đầu tư:", c.tieuDeGanNhat)
        assertEquals(2, c.soDongLienTiep)
        assertEquals("danh_sach", c.loaiGoiY)
        assertEquals(16, c.startIndex)
        assertEquals(75, c.endIndex)
    }

    @Test
    fun testQuestion1_PrefixHeadingVariants_ParenthesisAndRomanNumerals() {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 40, "b) Tư vấn giám sát: ....................\n"))
                    put(createParagraphElement(41, 80, "I. Ban quản lý dự án: ..................\n"))
                    put(createParagraphElement(81, 120, "2.1. Đơn vị thi công: ..................\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(3, candidates.size)

        assertEquals("b) Tư vấn giám sát:", candidates[0].tieuDeGanNhat)
        assertEquals("I. Ban quản lý dự án:", candidates[1].tieuDeGanNhat)
        assertEquals("2.1. Đơn vị thi công:", candidates[2].tieuDeGanNhat)
    }

    // =========================================================================
    // QUESTION 2: Does "* Máy móc thiết bị:" with dots avoid being dropped?
    // =========================================================================

    @Test
    fun testQuestion2_AsteriskEquipmentWithDotsNotDropped() = runBlocking {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 60, "* Máy móc thiết bị thi công chính: ....................\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(1, candidates.size)
        val c = candidates[0]

        assertTrue("Heading must contain Máy móc thiết bị", c.tieuDeGanNhat.contains("Máy móc thiết bị"))
        assertEquals("dong_cham", c.tinHieu)

        // Verify FormLlmDescriber classifies it as MAY_MOC and nhap_tay (NOT CONG_VIEC_THI_CONG)
        val describer = FormLlmDescriber()
        val described = describer.describeCandidates(candidates)
        assertEquals(1, described.size)
        val d = described[0]

        assertEquals("MAY_MOC", d.ma)
        assertEquals("nhap_tay", d.nguon)
        assertEquals("Máy móc thiết bị thi công", d.ten)
        assertFalse("Must NOT be classified as CONG_VIEC_THI_CONG", d.ma == "CONG_VIEC_THI_CONG")
    }

    @Test
    fun testQuestion2_BulletVariationsForEquipment() = runBlocking {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 50, "- Thiết bị thi công: ....................\n"))
                    put(createParagraphElement(51, 100, "+ Xe máy chuyên dùng: ....................\n"))
                    put(createParagraphElement(101, 150, "• Máy móc phục vụ: ....................\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(3, candidates.size)

        val describer = FormLlmDescriber()
        val described = describer.describeCandidates(candidates)

        for (d in described) {
            assertTrue("Should start with MAY_MOC", d.ma.startsWith("MAY_MOC"))
            assertEquals("nhap_tay", d.nguon)
        }
    }

    // =========================================================================
    // QUESTION 3: Does Section 3 heading bleed into Section 6 under any conditions?
    // =========================================================================

    @Test
    fun testQuestion3_Section3DoesNotBleedIntoSection6() = runBlocking {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    var idx = 1
                    fun addP(text: String): JSONObject {
                        val s = idx
                        val e = idx + text.length
                        idx = e
                        return createParagraphElement(s, e, text)
                    }

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
        val describer = FormLlmDescriber()
        val described = describer.describeCandidates(candidates)

        val cdt = described.find { it.ma == "Y_KIEN_CDT" }
        assertNotNull("Ý kiến CĐT must be present", cdt)
        assertFalse("Section 3 must NOT bleed into CDT", cdt!!.tieuDeGanNhat.contains("Công việc thi công"))
        assertTrue("CDT heading must be detected", cdt.tieuDeGanNhat.contains("Chủ đầu tư"))
        assertEquals("y_kien_cdt", cdt.nguon)

        val tvgs = described.find { it.ma == "Y_KIEN_TVGS" }
        assertNotNull("Ý kiến TVGS must be present", tvgs)
        assertFalse("Section 3 must NOT bleed into TVGS", tvgs!!.tieuDeGanNhat.contains("Công việc thi công"))
        assertTrue("TVGS heading must be detected", tvgs.tieuDeGanNhat.contains("Tư vấn giám sát"))
        assertEquals("y_kien_tvgs", tvgs.nguon)

        val nhaThau = described.find { it.ma == "Y_KIEN_NHA_THAU" }
        assertNotNull("Ý kiến Nhà thầu must be present", nhaThau)
        assertFalse("Section 3 must NOT bleed into Nha Thau", nhaThau!!.tieuDeGanNhat.contains("Công việc thi công trong ngày"))
        assertTrue("Nha Thau heading must be detected", nhaThau.tieuDeGanNhat.contains("Nhà thầu"))
        assertEquals("y_kien_nha_thau", nhaThau.nguon)
    }

    @Test
    fun testQuestion3_Section6WithoutColonDoesNotBleed() = runBlocking {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 35, "3. Công việc thi công trong ngày:\n"))
                    put(createParagraphElement(36, 70, "..................................\n"))
                    // Section 6 without trailing colon
                    put(createParagraphElement(71, 100, "6. Ý kiến của các đơn vị\n"))
                    put(createParagraphElement(101, 140, "a. Chủ đầu tư: ....................\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(2, candidates.size)

        val s6Candidate = candidates[1]
        assertFalse("Section 3 must NOT bleed into Section 6 without colon", s6Candidate.tieuDeGanNhat.contains("thi công"))
        assertTrue("CDT must be detected", s6Candidate.tieuDeGanNhat.contains("Chủ đầu tư"))
    }

    // =========================================================================
    // QUESTION 4: Tables with multi-paragraph cells, missing headers, empty cells
    // =========================================================================

    @Test
    fun testQuestion4_TableMultiParagraphCell() {
        val table = createTableElement(
            listOf(
                listOf(
                    listOf(
                        Pair(10, "Ý kiến Tư vấn giám sát:\n"),
                        Pair(35, "................................\n"),
                        Pair(70, "................................\n")
                    )
                )
            )
        )
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply { put(table) })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(1, candidates.size)
        val c = candidates[0]

        assertEquals("Ý kiến Tư vấn giám sát:", c.tieuDeGanNhat)
        assertEquals("Ý kiến Tư vấn giám sát:", c.truoc)
        assertEquals("o_bang", c.tinHieu)
        assertEquals(2, c.soDongLienTiep)
        assertEquals("danh_sach", c.loaiGoiY)
    }

    @Test
    fun testQuestion4_TableMissingColumnHeadersAndEmptyCells() {
        val table = createTableElement(
            listOf(
                // Row 0: Column 0 has header, Column 1 is empty (missing header), Column 2 has header
                listOf(
                    listOf(Pair(10, "STT\n")),
                    listOf(Pair(20, "\n")),
                    listOf(Pair(30, "Nội dung\n"))
                ),
                // Row 1: Completely empty cells and dot cells
                listOf(
                    listOf(Pair(40, "1\n")),
                    listOf(Pair(50, "\n")), // Empty cell
                    listOf(Pair(60, "....................\n")) // Dot cell
                )
            )
        )
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply { put(table) })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        // Candidates should include empty cell (Row 1 Col 1) and dot cell (Row 1 Col 2)
        assertTrue("Must scan without crash and find table candidates", candidates.isNotEmpty())

        val emptyCellCand = candidates.find { it.startIndex == 50 }
        assertNotNull("Empty cell candidate should be captured", emptyCellCand)
        assertEquals("o_bang", emptyCellCand?.tinHieu)

        val dotCellCand = candidates.find { it.startIndex == 60 }
        assertNotNull("Dot cell candidate should be captured", dotCellCand)
        assertEquals("o_bang", dotCellCand?.tinHieu)
    }

    @Test
    fun testQuestion4_TableWithJaggedRowsAndNullContentSafety() {
        // Construct malformed table JSON: row with 0 cells, cell with null content
        val malformedTable = JSONObject().apply {
            val rows = JSONArray()
            // Empty row
            rows.put(JSONObject().apply { put("tableCells", JSONArray()) })
            // Row with cell missing "content"
            rows.put(JSONObject().apply {
                put("tableCells", JSONArray().apply {
                    put(JSONObject().apply { put("startIndex", 100) })
                })
            })
            // Normal row
            rows.put(JSONObject().apply {
                put("tableCells", JSONArray().apply {
                    put(JSONObject().apply {
                        put("content", JSONArray().apply {
                            put(createParagraphElement(200, 230, "...............\n"))
                        })
                    })
                })
            })
            put("tableRows", rows)
        }

        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(JSONObject().apply { put("table", malformedTable) })
                })
            })
        }

        // Must execute gracefully without any crash
        val candidates = FormCandidateScanner.scanDocument(doc)
        assertTrue("Handled malformed table gracefully", candidates.isNotEmpty())
    }

    // =========================================================================
    // QUESTION 5: User {{MA}} tags detection and DocPlaceholderInjector 0 requests
    // =========================================================================

    @Test
    fun testQuestion5_UserDefinedTagsDetectedProperly() {
        val doc = JSONObject().apply {
            put("body", JSONObject().apply {
                put("content", JSONArray().apply {
                    put(createParagraphElement(1, 40, "Ý kiến Chủ đầu tư: {{Y_KIEN_CDT}}\n"))
                    put(createParagraphElement(41, 80, "Máy móc thi công: {{MAY_MOC}}\n"))
                    put(createParagraphElement(81, 120, "Tự định nghĩa: {{CUSTOM_TAG_123}}\n"))
                })
            })
        }

        val candidates = FormCandidateScanner.scanDocument(doc)
        assertEquals(3, candidates.size)

        for (c in candidates) {
            assertEquals("the_ma", c.tinHieu)
            assertEquals(1.0f, c.doTinCay, 0.001f)
            assertEquals(c.ma, c.originalMa)
        }

        assertEquals("Y_KIEN_CDT", candidates[0].ma)
        assertEquals("MAY_MOC", candidates[1].ma)
        assertEquals("CUSTOM_TAG_123", candidates[2].ma)
    }

    @Test
    fun testQuestion5_DocPlaceholderInjectorZeroRequestsWhenTagMatches() {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 25,
                truoc = "Ý kiến:",
                sau = "",
                tinHieu = "the_ma",
                tieuDeGanNhat = "Chủ đầu tư",
                loaiGoiY = "van_ban",
                ma = "Y_KIEN_CDT",
                originalMa = "Y_KIEN_CDT"
            ),
            FormCandidate(
                id = 2,
                startIndex = 50,
                endIndex = 62,
                truoc = "Máy móc:",
                sau = "",
                tinHieu = "the_ma",
                tieuDeGanNhat = "Máy móc",
                loaiGoiY = "van_ban",
                ma = "MAY_MOC",
                originalMa = "MAY_MOC"
            )
        )

        val payload = DocPlaceholderInjector.buildBatchUpdatePayload(candidates)
        val requests = payload.getJSONArray("requests")

        // Crucial requirement: 0 Google Docs API requests when tag matches
        assertEquals("Matching user tags must produce exactly 0 requests", 0, requests.length())
    }

    @Test
    fun testQuestion5_DocPlaceholderInjectorProducesRequestsWhenTagModified() {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 25,
                truoc = "Ý kiến:",
                sau = "",
                tinHieu = "the_ma",
                tieuDeGanNhat = "Chủ đầu tư",
                loaiGoiY = "van_ban",
                ma = "Y_KIEN_CHU_DAU_TU", // User renamed tag on App
                originalMa = "Y_KIEN_CDT"
            ),
            FormCandidate(
                id = 2,
                startIndex = 50,
                endIndex = 62,
                truoc = "Máy móc:",
                sau = "",
                tinHieu = "the_ma",
                tieuDeGanNhat = "Máy móc",
                loaiGoiY = "van_ban",
                ma = "MAY_MOC", // Unchanged
                originalMa = "MAY_MOC"
            )
        )

        val payload = DocPlaceholderInjector.buildBatchUpdatePayload(candidates)
        val requests = payload.getJSONArray("requests")

        // Candidate 2 unchanged -> 0 requests. Candidate 1 changed -> 1 delete + 1 insert = 2 requests
        assertEquals(2, requests.length())

        val deleteReq = requests.getJSONObject(0).getJSONObject("deleteContentRange").getJSONObject("range")
        assertEquals(10, deleteReq.getInt("startIndex"))
        assertEquals(25, deleteReq.getInt("endIndex"))

        val insertReq = requests.getJSONObject(1).getJSONObject("insertText")
        assertEquals(10, insertReq.getJSONObject("location").getInt("index"))
        assertEquals("{{Y_KIEN_CHU_DAU_TU}}", insertReq.getString("text"))
    }
}
