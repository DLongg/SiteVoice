package com.sitevoice.app

import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.export.GoogleDocsExporter
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
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDocsExporterTest {

    @Test
    fun testExportSuccessWithDefaultTemplate() = runBlocking {
        val fakeInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            if (url.contains("/copy")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id": "new_created_doc_123", "name": "Nhật ký thi công 2026-10-20"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains(":batchUpdate")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"documentId": "new_created_doc_123"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains("/documents/new_created_doc_123")) {
                // Pass 2 GET document: tài liệu sạch, không còn placeholder
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"documentId": "new_created_doc_123", "body": {"content": []}}""".toResponseBody("application/json".toMediaType()))
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

        val client = OkHttpClient.Builder()
            .addInterceptor(fakeInterceptor)
            .build()

        val exporter = GoogleDocsExporter(client)
        val dailyInput = DailyInputEntity(
            date = "2026-10-20",
            weatherMorning = "nang",
            weatherAfternoon = "nang",
            weatherEvening = "binh_thuong",
            commandStaffCount = 3,
            workerCount = 20,
            equipmentList = "1 cẩu tháp"
        )
        val reportContent = DailyReportContent(
            congViecThiCong = listOf("Đổ bê tông sàn tầng 2"),
            yKienCdt = "Đảm bảo an toàn"
        )

        val result = exporter.exportReport(
            date = "2026-10-20",
            dailyInput = dailyInput,
            reportContent = reportContent,
            templateDocId = "template_id_123",
            accessToken = "fake_access_token"
        )

        assertTrue(result.isSuccess)
        val export = result.getOrThrow()
        assertEquals("new_created_doc_123", export.docId)
        assertEquals("https://docs.google.com/document/d/new_created_doc_123/edit", export.docUrl)
    }

    @Test
    fun testExportSuccessWithCustomTemplate() = runBlocking {
        val fakeInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            if (url.contains("/copy")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id": "doc_personal_456", "name": "Nhật ký cá nhân 2026-10-20"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains(":batchUpdate")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"documentId": "doc_personal_456"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains("/documents/doc_personal_456")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"documentId": "doc_personal_456", "body": {"content": []}}""".toResponseBody("application/json".toMediaType()))
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

        val client = OkHttpClient.Builder()
            .addInterceptor(fakeInterceptor)
            .build()

        val exporter = GoogleDocsExporter(client)
        val customTemplate = TemplateItemEntity(
            id = "nhat-ky-ca-nhan",
            name = "Nhật ký cá nhân",
            description = "Báo cáo cá nhân",
            systemPrompt = "Prompt",
            schemaJson = """{"summaryFields": [{"name": "tam_trang", "type": "string"}]}"""
        )

        val reportContent = DailyReportContent(
            allFields = mapOf("tam_trang" to "Tuyệt vời")
        )

        val result = exporter.exportReport(
            date = "2026-10-20",
            dailyInput = null,
            reportContent = reportContent,
            templateDocId = "template_custom_999",
            accessToken = "fake_access_token",
            template = customTemplate
        )

        assertTrue(result.isSuccess)
        val export = result.getOrThrow()
        assertEquals("doc_personal_456", export.docId)
        assertEquals("Nhật ký cá nhân 2026-10-20", export.title)
    }

    @Test
    fun testExportTwoPassCleansLingeringTags() = runBlocking {
        var batchUpdateCallCount = 0
        var secondBatchPayload: String? = null

        val fakeInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            if (url.contains("/copy")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id": "doc_lingering_789", "name": "Báo cáo 2026-10-24"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains(":batchUpdate")) {
                batchUpdateCallCount++
                if (batchUpdateCallCount == 2) {
                    val buffer = okio.Buffer()
                    request.body?.writeTo(buffer)
                    secondBatchPayload = buffer.readUtf8()
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"documentId": "doc_lingering_789"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains("/documents/doc_lingering_789")) {
                // Trả về tài liệu sau Pass 1 vẫn còn chứa các thẻ chưa điền
                val docJsonWithResiduals = """
                {
                    "documentId": "doc_lingering_789",
                    "body": {
                        "content": [
                            {
                                "paragraph": {
                                    "elements": [
                                        {
                                            "textRun": {
                                                "content": "Phần mở đầu còn sót thẻ {{UNFILLED_FIELD_1}} ở đây.\n"
                                            }
                                        }
                                    ]
                                }
                            },
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
                                                                    {
                                                                        "textRun": {
                                                                            "content": "Ô bảng chứa {{ UNFILLED_FIELD_2 }}\n"
                                                                        }
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
                """.trimIndent()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(docJsonWithResiduals.toResponseBody("application/json".toMediaType()))
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

        val client = OkHttpClient.Builder()
            .addInterceptor(fakeInterceptor)
            .build()

        val exporter = GoogleDocsExporter(client)
        val result = exporter.exportReport(
            date = "2026-10-24",
            dailyInput = null,
            reportContent = DailyReportContent(),
            templateDocId = "template_id_123",
            accessToken = "fake_access_token"
        )

        assertTrue(result.isSuccess)
        // Xác nhận Pass 2 đã kích hoạt lời gọi batchUpdate thứ hai
        assertEquals(2, batchUpdateCallCount)
        assertTrue(secondBatchPayload != null)

        val batchJson = JSONObject(secondBatchPayload!!)
        val requests = batchJson.getJSONArray("requests")
        assertEquals(2, requests.length())

        val replacedTags = mutableSetOf<String>()
        for (i in 0 until requests.length()) {
            val req = requests.getJSONObject(i)
            val replaceAllText = req.getJSONObject("replaceAllText")
            val containsText = replaceAllText.getJSONObject("containsText")
            val text = containsText.getString("text")
            val replaceText = replaceAllText.getString("replaceText")

            replacedTags.add(text)
            assertEquals("", replaceText)
        }

        assertTrue(replacedTags.contains("{{UNFILLED_FIELD_1}}"))
        assertTrue(replacedTags.contains("{{ UNFILLED_FIELD_2 }}"))
    }

    @Test
    fun testExportPass2SkippedWhenNoLingeringTags() = runBlocking {
        var batchUpdateCallCount = 0

        val fakeInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            if (url.contains("/copy")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id": "doc_clean_999", "name": "Báo cáo sạch 2026-10-24"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains(":batchUpdate")) {
                batchUpdateCallCount++
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"documentId": "doc_clean_999"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else if (url.contains("/documents/doc_clean_999")) {
                // Trả về tài liệu sạch, không còn placeholder nào
                val cleanDocJson = """
                {
                    "documentId": "doc_clean_999",
                    "body": {
                        "content": [
                            {
                                "paragraph": {
                                    "elements": [
                                        {
                                            "textRun": {
                                                "content": "Báo cáo hoàn toàn sạch sẽ, không còn thẻ nào.\n"
                                            }
                                        }
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
                    .body(cleanDocJson.toResponseBody("application/json".toMediaType()))
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

        val client = OkHttpClient.Builder()
            .addInterceptor(fakeInterceptor)
            .build()

        val exporter = GoogleDocsExporter(client)
        val result = exporter.exportReport(
            date = "2026-10-24",
            dailyInput = null,
            reportContent = DailyReportContent(),
            templateDocId = "template_id_clean",
            accessToken = "fake_access_token"
        )

        assertTrue(result.isSuccess)
        // Pass 2 bỏ qua vì không có placeholder sót -> batchUpdate chỉ chạy 1 lần duy nhất (Pass 1)
        assertEquals(1, batchUpdateCallCount)
    }

    @Test
    fun testFindLingeringPlaceholdersInHeadersFootersAndTables() {
        val docJson = JSONObject("""
        {
            "body": {
                "content": [
                    {
                        "paragraph": {
                            "elements": [
                                { "textRun": { "content": "Thân bài: {{BODY_TAG_1}} và {{BODY_TAG_2}}" } }
                            ]
                        }
                    },
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
                                                            { "textRun": { "content": "Bảng: {{TABLE_CELL_TAG}}" } }
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
            },
            "headers": {
                "header_1": {
                    "content": [
                        {
                            "paragraph": {
                                "elements": [
                                    { "textRun": { "content": "Tiêu đề: {{HEADER_TAG}}" } }
                                ]
                            }
                        }
                    ]
                }
            },
            "footers": {
                "footer_1": {
                    "content": [
                        {
                            "paragraph": {
                                "elements": [
                                    { "textRun": { "content": "Chân trang: {{FOOTER_TAG}}" } }
                                ]
                            }
                        }
                    ]
                }
            },
            "footnotes": {
                "fn_1": {
                    "content": [
                        {
                            "paragraph": {
                                "elements": [
                                    { "textRun": { "content": "Chú thích: {{FOOTNOTE_TAG}}" } }
                                ]
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent())

        val found = GoogleDocsExporter.findLingeringPlaceholders(docJson)
        assertEquals(
            setOf("{{BODY_TAG_1}}", "{{BODY_TAG_2}}", "{{TABLE_CELL_TAG}}", "{{HEADER_TAG}}", "{{FOOTER_TAG}}", "{{FOOTNOTE_TAG}}"),
            found
        )
    }

    @Test
    fun testExportCopyFailure() = runBlocking {
        val fakeInterceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(403)
                .message("Forbidden")
                .body("""{"error": {"message": "Permission denied"}}""".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(fakeInterceptor)
            .build()

        val exporter = GoogleDocsExporter(client)
        val result = exporter.exportReport(
            date = "2026-10-20",
            dailyInput = null,
            reportContent = DailyReportContent(),
            templateDocId = "invalid_id",
            accessToken = "fake_token"
        )

        assertTrue(result.isFailure)
    }
}
