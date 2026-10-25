package com.sitevoice.app

import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.export.PlaceholderMapper
import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormCandidateScanner
import com.sitevoice.app.formsetup.FormLlmDescriber
import com.sitevoice.app.formsetup.FormSetupService
import com.sitevoice.app.llm.LlmClient
import com.sitevoice.app.llm.LlmMessage
import com.sitevoice.app.template.DailyReportContent
import com.sitevoice.app.template.TemplateSchema
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengerM1_2AdversarialTest {

    // =========================================================================
    // 1. Markdown Fences in callLlm: ```json, ```JSON, ```, CRLF
    // =========================================================================

    private fun createFakeClientWithResponse(responsePayload: String): LlmClient {
        return object : LlmClient {
            override suspend fun chat(
                messages: List<LlmMessage>,
                temperature: Float,
                responseJson: Boolean
            ): Result<String> = Result.success(responsePayload)
        }
    }

    @Test
    fun testCallLlmHandlesLowercaseJsonFence() = runBlocking {
        val payload = """
            ```json
            {
              "ket_qua": [
                {
                  "id": 1,
                  "ma": "CONG_VIEC_THI_CONG",
                  "ten": "Thi công",
                  "loai": "danh_sach",
                  "nguon": "thi_cong",
                  "bo_qua": false,
                  "do_tin_cay": 0.95
                }
              ]
            }
            ```
        """.trimIndent()

        val describer = FormLlmDescriber(createFakeClientWithResponse(payload))
        val candidate = FormCandidate(id = 1, startIndex = 0, endIndex = 10, truoc = "A", sau = "B", tinHieu = "dong_cham", tieuDeGanNhat = "C", loaiGoiY = "van_ban")
        val result = describer.describeCandidates(listOf(candidate))

        assertEquals(1, result.size)
        assertEquals("CONG_VIEC_THI_CONG", result[0].ma)
        assertEquals("thi_cong", result[0].nguon)
    }

    @Test
    fun testCallLlmHandlesUppercaseJsonFence() = runBlocking {
        val payload = """
            ```JSON
            {
              "ket_qua": [
                {
                  "id": 1,
                  "ma": "NGHIEM_THU",
                  "ten": "Nghiệm thu",
                  "loai": "danh_sach",
                  "nguon": "nghiem_thu",
                  "bo_qua": false,
                  "do_tin_cay": 0.96
                }
              ]
            }
            ```
        """.trimIndent()

        val describer = FormLlmDescriber(createFakeClientWithResponse(payload))
        val candidate = FormCandidate(id = 1, startIndex = 0, endIndex = 10, truoc = "A", sau = "B", tinHieu = "dong_cham", tieuDeGanNhat = "C", loaiGoiY = "van_ban")
        val result = describer.describeCandidates(listOf(candidate))

        assertEquals(1, result.size)
        assertEquals("NGHIEM_THU", result[0].ma)
        assertEquals("nghiem_thu", result[0].nguon)
    }

    @Test
    fun testCallLlmHandlesBareFence() = runBlocking {
        val payload = """
            ```
            {
              "ket_qua": [
                {
                  "id": 1,
                  "ma": "AN_TOAN",
                  "ten": "An toàn",
                  "loai": "danh_sach",
                  "nguon": "an_toan",
                  "bo_qua": false,
                  "do_tin_cay": 0.94
                }
              ]
            }
            ```
        """.trimIndent()

        val describer = FormLlmDescriber(createFakeClientWithResponse(payload))
        val candidate = FormCandidate(id = 1, startIndex = 0, endIndex = 10, truoc = "A", sau = "B", tinHieu = "dong_cham", tieuDeGanNhat = "C", loaiGoiY = "van_ban")
        val result = describer.describeCandidates(listOf(candidate))

        assertEquals(1, result.size)
        assertEquals("AN_TOAN", result[0].ma)
        assertEquals("an_toan", result[0].nguon)
    }

    @Test
    fun testCallLlmHandlesWindowsCrlfNewlinesWithFence() = runBlocking {
        val payload = "```json\r\n{\r\n  \"ket_qua\": [\r\n    {\r\n      \"id\": 1,\r\n      \"ma\": \"MAY_MOC\",\r\n      \"ten\": \"Máy móc\",\r\n      \"loai\": \"van_ban\",\r\n      \"nguon\": \"nhap_tay\",\r\n      \"bo_qua\": false,\r\n      \"do_tin_cay\": 0.92\r\n    }\r\n  ]\r\n}\r\n```"

        val describer = FormLlmDescriber(createFakeClientWithResponse(payload))
        val candidate = FormCandidate(id = 1, startIndex = 0, endIndex = 10, truoc = "A", sau = "B", tinHieu = "dong_cham", tieuDeGanNhat = "C", loaiGoiY = "van_ban")
        val result = describer.describeCandidates(listOf(candidate))

        assertEquals(1, result.size)
        assertEquals("MAY_MOC", result[0].ma)
    }

    // =========================================================================
    // 2. Deterministic Rule Priorities
    // =========================================================================

    @Test
    fun testNhaThauThiCongClassifiedAsYKienNhaThauNotCongViecThiCong() {
        val describer = FormLlmDescriber()

        // Test with tieuDeGanNhat = "Nhà thầu thi công"
        val c1 = describer.applyDeterministicRules(
            FormCandidate(
                id = 1, startIndex = 100, endIndex = 110,
                truoc = "", sau = "", tinHieu = "dong_cham",
                tieuDeGanNhat = "Nhà thầu thi công", loaiGoiY = "van_ban"
            )
        )
        assertEquals("Y_KIEN_NHA_THAU", c1.ma)
        assertEquals("y_kien_nha_thau", c1.nguon)
        assertEquals("van_ban", c1.loai)
        assertFalse("Must not be CONG_VIEC_THI_CONG", c1.ma == "CONG_VIEC_THI_CONG")

        // Test with truoc = "c. Nhà thầu thi công xây dựng:"
        val c2 = describer.applyDeterministicRules(
            FormCandidate(
                id = 2, startIndex = 150, endIndex = 160,
                truoc = "c. Nhà thầu thi công xây dựng:", sau = "", tinHieu = "dong_cham",
                tieuDeGanNhat = "6. Ý kiến của các đơn vị:", loaiGoiY = "van_ban"
            )
        )
        assertEquals("Y_KIEN_NHA_THAU", c2.ma)
        assertEquals("y_kien_nha_thau", c2.nguon)
    }

    @Test
    fun testMayMocThietBiThiCongClassifiedAsMayMocNotCongViecThiCong() {
        val describer = FormLlmDescriber()

        // Test with tieuDeGanNhat = "Máy móc thiết bị thi công"
        val c1 = describer.applyDeterministicRules(
            FormCandidate(
                id = 1, startIndex = 200, endIndex = 210,
                truoc = "", sau = "", tinHieu = "dong_cham",
                tieuDeGanNhat = "Máy móc thiết bị thi công", loaiGoiY = "van_ban"
            )
        )
        assertEquals("MAY_MOC", c1.ma)
        assertEquals("nhap_tay", c1.nguon)
        assertEquals("van_ban", c1.loai)
        assertFalse("Must not be CONG_VIEC_THI_CONG", c1.ma == "CONG_VIEC_THI_CONG")

        // Test with truoc = "* Máy móc thiết bị thi công chính:"
        val c2 = describer.applyDeterministicRules(
            FormCandidate(
                id = 2, startIndex = 250, endIndex = 260,
                truoc = "* Máy móc thiết bị thi công chính:", sau = "", tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Lực lượng thi công", loaiGoiY = "van_ban"
            )
        )
        assertEquals("MAY_MOC", c2.ma)
        assertEquals("nhap_tay", c2.nguon)

        // Test with truoc = "Xe máy thi công:"
        val c3 = describer.applyDeterministicRules(
            FormCandidate(
                id = 3, startIndex = 300, endIndex = 310,
                truoc = "Xe máy thi công:", sau = "", tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Lực lượng thi công", loaiGoiY = "van_ban"
            )
        )
        assertEquals("MAY_MOC", c3.ma)
        assertEquals("nhap_tay", c3.nguon)
    }

    @Test
    fun testMultipleEquipmentCandidatesDeduplicatedCleanly() = runBlocking {
        val describer = FormLlmDescriber()
        val candidates = listOf(
            FormCandidate(id = 1, startIndex = 0, endIndex = 10, truoc = "Thiết bị 1:", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "Thiết bị thi công", loaiGoiY = "van_ban"),
            FormCandidate(id = 2, startIndex = 20, endIndex = 30, truoc = "Thiết bị 2:", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "Thiết bị thi công", loaiGoiY = "van_ban"),
            FormCandidate(id = 3, startIndex = 40, endIndex = 50, truoc = "Thiết bị 3:", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "Thiết bị thi công", loaiGoiY = "van_ban")
        )

        val described = describer.describeCandidates(candidates)
        assertEquals(3, described.size)
        assertEquals("MAY_MOC", described[0].ma)
        assertEquals("MAY_MOC_2", described[1].ma)
        assertEquals("MAY_MOC_3", described[2].ma)

        // All retain correct classification source
        for (d in described) {
            assertEquals("nhap_tay", d.nguon)
            assertEquals("van_ban", d.loai)
        }
    }

    // =========================================================================
    // 3. Equipment Fallback in PlaceholderMapper
    // =========================================================================

    @Test
    fun testPlaceholderMapperEquipmentFallbackWhenDailyInputNullOrBlank() {
        val reportContent = DailyReportContent(
            allFields = mapOf("may_moc" to "2 máy xúc đào, 1 lu rung")
        )

        // Case 1: dailyInput is null
        val map1 = PlaceholderMapper.buildPlaceholderMap("2026-10-25", null, reportContent)
        assertEquals("2 máy xúc đào, 1 lu rung", map1["{{MAY_MOC}}"])
        assertEquals("2 máy xúc đào, 1 lu rung", map1["{{may_moc}}"])
        assertEquals("2 máy xúc đào, 1 lu rung", map1["{{THIET_BI}}"])
        assertEquals("2 máy xúc đào, 1 lu rung", map1["{{thiet_bi}}"])

        // Case 2: dailyInput exists but equipmentList is empty string
        val dailyInputEmpty = DailyInputEntity(date = "2026-10-25", equipmentList = "")
        val map2 = PlaceholderMapper.buildPlaceholderMap("2026-10-25", dailyInputEmpty, reportContent)
        assertEquals("2 máy xúc đào, 1 lu rung", map2["{{MAY_MOC}}"])
        assertEquals("2 máy xúc đào, 1 lu rung", map2["{{THIET_BI}}"])

        // Case 3: dailyInput exists but equipmentList is whitespace only
        val dailyInputSpaces = DailyInputEntity(date = "2026-10-25", equipmentList = "    ")
        val map3 = PlaceholderMapper.buildPlaceholderMap("2026-10-25", dailyInputSpaces, reportContent)
        assertEquals("2 máy xúc đào, 1 lu rung", map3["{{MAY_MOC}}"])

        // Case 4: dailyInput has equipment, should NOT be overridden by reportContent
        val dailyInputValid = DailyInputEntity(date = "2026-10-25", equipmentList = "1 cẩu tháp 50m")
        val map4 = PlaceholderMapper.buildPlaceholderMap("2026-10-25", dailyInputValid, reportContent)
        assertEquals("1 cẩu tháp 50m", map4["{{MAY_MOC}}"])
        assertEquals("1 cẩu tháp 50m", map4["{{THIET_BI}}"])

        // Case 5: reportContent has thiet_bi key instead of may_moc
        val contentThietBi = DailyReportContent(
            allFields = mapOf("thiet_bi" to "3 xe ben chuyên dụng")
        )
        val map5 = PlaceholderMapper.buildPlaceholderMap("2026-10-25", null, contentThietBi)
        assertEquals("3 xe ben chuyên dụng", map5["{{MAY_MOC}}"])
        assertEquals("3 xe ben chuyên dụng", map5["{{THIET_BI}}"])

        // Case 6: reportContent has equipment as a list (standard text bullets)
        val contentList = DailyReportContent(
            allFields = mapOf("may_moc" to listOf("Máy bơm bê tông", "Xe bồn"))
        )
        val map6 = PlaceholderMapper.buildPlaceholderMap("2026-10-25", null, contentList)
        assertEquals("- Máy bơm bê tông\n- Xe bồn", map6["{{MAY_MOC}}"])

        // Case 7: Note that formatList regex treats leading digits followed by space ("1 máy...") as numbered section headers
        val contentNumericList = DailyReportContent(
            allFields = mapOf("may_moc" to listOf("1 máy bơm", "2 xe bồn"))
        )
        val map7 = PlaceholderMapper.buildPlaceholderMap("2026-10-25", null, contentNumericList)
        assertEquals("1 máy bơm\n2 xe bồn", map7["{{MAY_MOC}}"])
    }

    // =========================================================================
    // 4. Position Coordinates Preservation Across Serialization
    // =========================================================================

    @Test
    fun testPositionCoordinatesPreservedInJsonMapFormat() {
        val json = """
            {
              "id": "tpl_test_pos_map",
              "ten": "Mẫu Map",
              "truong_tong_hop": {
                "cong_viec_thi_cong": {
                  "kieu": "danh_sach",
                  "start_index": 150,
                  "end_index": 220,
                  "tieu_de_gan_nhat": "3. Công việc thi công trong ngày:",
                  "truoc": "Hạng mục phần thân:",
                  "sau": "4. Nghiệm thu",
                  "tin_hieu": "dong_cham"
                }
              }
            }
        """.trimIndent()

        val schema = TemplateSchema.fromJson(json)
        val field = schema.summaryFields["cong_viec_thi_cong"]
        assertNotNull(field)
        assertEquals(150, field?.startIndex)
        assertEquals(220, field?.endIndex)
        assertEquals("3. Công việc thi công trong ngày:", field?.tieuDeGanNhat)
        assertEquals("Hạng mục phần thân:", field?.truoc)
        assertEquals("4. Nghiệm thu", field?.sau)
        assertEquals("dong_cham", field?.tinHieu)
    }

    @Test
    fun testPositionCoordinatesPreservedInJsonArrayFormat() {
        val json = """
            {
              "id": "tpl_test_pos_arr",
              "name": "Mẫu Array",
              "summary_fields": [
                {
                  "key": "an_toan",
                  "type": "danh_sach",
                  "startIndex": 300,
                  "endIndex": 350,
                  "tieuDeGanNhat": "5. An toàn lao động",
                  "before": "Vệ sinh công trường:",
                  "after": "6. Ý kiến",
                  "tinHieu": "doan_trong"
                }
              ]
            }
        """.trimIndent()

        val schema = TemplateSchema.fromJson(json)
        val field = schema.summaryFields["an_toan"]
        assertNotNull(field)
        assertEquals(300, field?.startIndex)
        assertEquals(350, field?.endIndex)
        assertEquals("5. An toàn lao động", field?.tieuDeGanNhat)
        assertEquals("Vệ sinh công trường:", field?.truoc)
        assertEquals("6. Ý kiến", field?.sau)
        assertEquals("doan_trong", field?.tinHieu)
    }

    @Test
    fun testFormSetupServiceFullRoundtripPreservesPositionCoordinates() = runBlocking {
        val candidate = FormCandidate(
            id = 1,
            startIndex = 450,
            endIndex = 520,
            truoc = "2. Nhân lực & máy móc:",
            sau = "3. Thi công",
            tinHieu = "dong_cham",
            tieuDeGanNhat = "2. Nhân lực & máy móc:",
            soDongLienTiep = 3,
            loaiGoiY = "van_ban",
            ma = "MAY_MOC",
            ten = "Máy móc thiết bị thi công",
            loai = "van_ban",
            nguon = "nhap_tay"
        )

        val fakeClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"documentId": "pos_roundtrip_doc"}""".toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val service = FormSetupService(httpClient = fakeClient)
        val template = service.applyPlaceholdersAndGenerateTemplate(
            workingDocId = "pos_roundtrip_doc",
            templateName = "Mẫu Roundtrip Vị Trí",
            candidates = listOf(candidate),
            accessToken = "mock_token"
        ).getOrThrow()

        val schema = TemplateSchema.fromJson(template.schemaJson)
        val field = schema.summaryFields["may_moc"]
        assertNotNull(field)
        assertEquals(450, field?.startIndex)
        assertEquals(520, field?.endIndex)
        assertEquals("2. Nhân lực & máy móc:", field?.tieuDeGanNhat)
        assertEquals("2. Nhân lực & máy móc:", field?.truoc)
        assertEquals("3. Thi công", field?.sau)
        assertEquals("dong_cham", field?.tinHieu)
    }
}
