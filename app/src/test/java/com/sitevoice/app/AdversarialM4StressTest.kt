package com.sitevoice.app

import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.export.DailyLogEntity
import com.sitevoice.app.export.GoogleDocsExporter
import com.sitevoice.app.export.PlaceholderMapper
import com.sitevoice.app.template.DailyReportContent
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Empirical Adversarial Stress Test Suite for Milestone M4 (Requirement R4).
 * Focus areas:
 * 1. Text run concatenation in paragraphs preventing tag splitting across runs.
 * 2. Nested tables inside tables and empty table cell robustness.
 * 3. Network error resilience during GET /v1/documents and Pass 2 batchUpdate.
 * 4. Lingering tags containing whitespace, lowercase, mixed-case, and regex boundary conditions.
 * 5. Two-pass clean sweep end-to-end verification.
 */
class AdversarialM4StressTest {

    // =========================================================================
    // DOMAIN 1: Text Run Concatenation within Paragraphs
    // =========================================================================

    @Test
    fun testTextRunConcatenationPreventsTagSplittingAcrossRuns() {
        // Tag split across 4 distinct textRun fragments: "{{", "TIEN_DO_", "HOM_NAY", "}}"
        val docJson = JSONObject("""
        {
            "body": {
                "content": [
                    {
                        "paragraph": {
                            "elements": [
                                { "textRun": { "content": "Tình hình thi công: {{" } },
                                { "textRun": { "content": "TIEN_DO_" } },
                                { "textRun": { "content": "HOM_NAY" } },
                                { "textRun": { "content": "}} hoàn thành 100%.\n" } },
                                { "textRun": { "content": "Thêm một thẻ split khác: {{" } },
                                { "textRun": { "content": "AN_TOAN_VS}} tốt." } }
                            ]
                        }
                    }
                ]
            }
        }
        """.trimIndent())

        val foundTags = GoogleDocsExporter.findLingeringPlaceholders(docJson)

        assertEquals(2, foundTags.size)
        assertTrue("Must detect tag split across 4 runs", foundTags.contains("{{TIEN_DO_HOM_NAY}}"))
        assertTrue("Must detect tag split across 2 runs", foundTags.contains("{{AN_TOAN_VS}}"))
    }

    // =========================================================================
    // DOMAIN 2: Deeply Nested Tables & Empty Cell Robustness
    // =========================================================================

    @Test
    fun testNestedTablesAndEmptyCellRobustness() {
        // A 3-level deeply nested table alongside empty rows and cells
        val docJson = JSONObject("""
        {
            "body": {
                "content": [
                    {
                        "table": {
                            "tableRows": [
                                {
                                    "tableCells": [
                                        { "content": [] },
                                        { "content": null }
                                    ]
                                },
                                {
                                    "tableCells": [
                                        {
                                            "content": [
                                                {
                                                    "table": {
                                                        "tableRows": [
                                                            {
                                                                "tableCells": [
                                                                    {
                                                                        "content": [
                                                                            {
                                                                                "table": {
                                                                                    "tableRows": [
                                                                                        {
                                                                                            "tableCells": [
                                                                                                {
                                                                                                    "content": [
                                                                                                        {
                                                                                                            "paragraph": {
                                                                                                                "elements": [
                                                                                                                    { "textRun": { "content": "Nested: {{DEEP_NESTED_TAG}}" } }
                                                                                                                ]
                                                                                                            }
                                                                                                        }
                                                                                                    ]
                                                                                                }
                                                                                            ]
                                                                                        }
                                                                                    ]
                                                                                }
                                                                            }
                                                                        ]
                                                                    }
                                                                ]
                                                            }
                                                        ]
                                                    }
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    }
                ]
            }
        }
        """.trimIndent())

        val foundTags = GoogleDocsExporter.findLingeringPlaceholders(docJson)

        assertEquals(1, foundTags.size)
        assertTrue("Must discover tag inside 3-level deep nested table", foundTags.contains("{{DEEP_NESTED_TAG}}"))
    }

    @Test
    fun testEmptyDocAndEmptyCellsReturnEmptyWithoutException() {
        val emptyDocs = listOf(
            JSONObject("{}"),
            JSONObject("""{"body": null}"""),
            JSONObject("""{"body": {"content": []}}"""),
            JSONObject("""{"body": {"content": [{"table": {"tableRows": []}}]}}"""),
            JSONObject("""{"body": {"content": [{"paragraph": {"elements": []}}]}}""")
        )

        for (doc in emptyDocs) {
            val result = GoogleDocsExporter.findLingeringPlaceholders(doc)
            assertTrue("Empty doc structures must return empty set", result.isEmpty())
        }
    }

    // =========================================================================
    // DOMAIN 3: Lingering Tag Regex Permutations (Whitespace, Lowercase, Mixed)
    // =========================================================================

    @Test
    fun testLingeringTagRegexPermutationsAndExclusions() {
        val docJson = JSONObject("""
        {
            "body": {
                "content": [
                    {
                        "paragraph": {
                            "elements": [
                                { "textRun": { "content": "Tags: {{ tag_name }} and {{cong_viec_thi_cong}}.\n" } },
                                { "textRun": { "content": "Spacing: {{   SPACED_TAG   }} and {{MixEd_CaSe_123}}.\n" } },
                                { "textRun": { "content": "Dashes: {{FIELD-WITH-DASH_99}} and single brace {NOT_A_TAG}.\n" } },
                                { "textRun": { "content": "Empty: {{}} and multiline {{NOT_\n_MATCHED}}.\n" } }
                            ]
                        }
                    }
                ]
            }
        }
        """.trimIndent())

        val found = GoogleDocsExporter.findLingeringPlaceholders(docJson)

        assertTrue("Lowercase with spaces", found.contains("{{ tag_name }}"))
        assertTrue("Lowercase underscore", found.contains("{{cong_viec_thi_cong}}"))
        assertTrue("Multiple spaces", found.contains("{{   SPACED_TAG   }}"))
        assertTrue("Mixed case", found.contains("{{MixEd_CaSe_123}}"))
        assertTrue("Dashes and underscores", found.contains("{{FIELD-WITH-DASH_99}}"))

        // Exclusions
        assertFalse("Empty tag {{}} must be excluded", found.contains("{{}}"))
        assertFalse("Single brace must be excluded", found.contains("{NOT_A_TAG}"))
        assertFalse("Multiline tag must be excluded", found.any { it.contains("\n") })
    }

    // =========================================================================
    // DOMAIN 4: Network Error Handling during Pass 2 GET and batchUpdate
    // =========================================================================

    @Test
    fun testExportFailsGracefullyWhenGetDocumentReturnsNetworkError() = runBlocking {
        val fakeInterceptor = Interceptor { chain ->
            val url = chain.request().url.toString()
            if (url.contains("/copy")) {
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id": "doc_net_err_1", "name": "Report"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains(":batchUpdate")) {
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"documentId": "doc_net_err_1"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains("/documents/doc_net_err_1")) {
                // Network error simulated: 503 Service Unavailable
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(503)
                    .message("Service Unavailable")
                    .body("""{"error": "Backend timeout"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else {
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }

        val client = OkHttpClient.Builder().addInterceptor(fakeInterceptor).build()
        val exporter = GoogleDocsExporter(client)

        val result = exporter.exportReport(
            date = "2026-10-24",
            dailyInput = null,
            reportContent = DailyReportContent(),
            templateDocId = "tpl_id",
            accessToken = "token"
        )

        assertTrue("Result must be failure when GET /documents fails", result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("Exception must be IOException", ex is IOException)
        assertTrue("Error message must mention Pass 2 failure", ex?.message?.contains("Lỗi đọc tài liệu kiểm tra lớp 2") == true)
    }

    @Test
    fun testExportFailsGracefullyWhenPass2BatchUpdateFails() = runBlocking {
        var batchCount = 0
        val fakeInterceptor = Interceptor { chain ->
            val url = chain.request().url.toString()
            if (url.contains("/copy")) {
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id": "doc_batch2_err", "name": "Report"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains(":batchUpdate")) {
                batchCount++
                if (batchCount == 1) {
                    // Pass 1 batchUpdate succeeds
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("""{"documentId": "doc_batch2_err"}""".toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    // Pass 2 batchUpdate fails with 500 Internal Server Error
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(500)
                        .message("Internal Server Error")
                        .body("""{"error": "Internal Google error"}""".toResponseBody("application/json".toMediaType()))
                        .build()
                }
            } else if (url.contains("/documents/doc_batch2_err")) {
                // Pass 2 GET finds lingering tags
                val docJson = """
                {
                    "documentId": "doc_batch2_err",
                    "body": {
                        "content": [
                            {
                                "paragraph": {
                                    "elements": [
                                        { "textRun": { "content": "{{RESIDUAL_TAG}}" } }
                                    ]
                                }
                            }
                        ]
                    }
                }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(docJson.toResponseBody("application/json".toMediaType()))
                    .build()
            } else {
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }

        val client = OkHttpClient.Builder().addInterceptor(fakeInterceptor).build()
        val exporter = GoogleDocsExporter(client)

        val result = exporter.exportReport(
            date = "2026-10-24",
            dailyInput = null,
            reportContent = DailyReportContent(),
            templateDocId = "tpl_id",
            accessToken = "token"
        )

        assertTrue("Result must be failure when Pass 2 batchUpdate fails", result.isFailure)
        assertEquals(2, batchCount)
        val ex = result.exceptionOrNull()
        assertTrue(ex?.message?.contains("Lỗi quét sạch placeholder lớp 2") == true)
    }

    // =========================================================================
    // DOMAIN 5: End-to-End Pass 2 Clean Sweep with Whitespace & Lowercase Tags
    // =========================================================================

    @Test
    fun testTwoPassExportCleansWhitespaceAndLowercaseResidualTags() = runBlocking {
        var secondBatchBody: String? = null
        var batchCount = 0

        val fakeInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()
            if (url.contains("/copy")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id": "doc_e2e_clean", "name": "Report"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains(":batchUpdate")) {
                batchCount++
                if (batchCount == 2) {
                    val buffer = okio.Buffer()
                    request.body?.writeTo(buffer)
                    secondBatchBody = buffer.readUtf8()
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"documentId": "doc_e2e_clean"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains("/documents/doc_e2e_clean")) {
                val residualJson = """
                {
                    "documentId": "doc_e2e_clean",
                    "body": {
                        "content": [
                            {
                                "paragraph": {
                                    "elements": [
                                        { "textRun": { "content": "Residual 1: {{ unmapped_tag_1 }}\n" } },
                                        { "textRun": { "content": "Residual 2: {{MIXED_case_tag}}\n" } }
                                    ]
                                }
                            }
                        ]
                    }
                }
                """.trimIndent()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(residualJson.toResponseBody("application/json".toMediaType()))
                    .build()
            } else {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }

        val client = OkHttpClient.Builder().addInterceptor(fakeInterceptor).build()
        val exporter = GoogleDocsExporter(client)

        val result = exporter.exportReport(
            date = "2026-10-24",
            dailyInput = null,
            reportContent = DailyReportContent(),
            templateDocId = "template_id",
            accessToken = "token"
        )

        assertTrue(result.isSuccess)
        assertEquals(2, batchCount)
        assertTrue(secondBatchBody != null)

        val json = JSONObject(secondBatchBody!!)
        val requests = json.getJSONArray("requests")
        assertEquals(2, requests.length())

        val cleanedTags = mutableSetOf<String>()
        for (i in 0 until requests.length()) {
            val req = requests.getJSONObject(i).getJSONObject("replaceAllText")
            cleanedTags.add(req.getJSONObject("containsText").getString("text"))
            assertEquals("", req.getString("replaceText"))
            assertFalse(req.getJSONObject("containsText").getBoolean("matchCase"))
        }

        assertTrue(cleanedTags.contains("{{ unmapped_tag_1 }}"))
        assertTrue(cleanedTags.contains("{{MIXED_case_tag}}"))
    }
}
