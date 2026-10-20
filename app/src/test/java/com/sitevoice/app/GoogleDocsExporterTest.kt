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
