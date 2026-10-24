package com.sitevoice.app

import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormSetupService
import com.sitevoice.app.template.TemplateSchema
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File

class TemplateSchemaTest {

    @Test
    fun testParseSchemaJson() {
        val schemaFile = File("src/main/assets/templates/nhat-ky-cong-truong/schema.json")
        val altFile = File("app/src/main/assets/templates/nhat-ky-cong-truong/schema.json")
        val target = if (schemaFile.exists()) schemaFile else altFile

        assertTrue("schema.json must exist in assets", target.exists())

        val jsonContent = target.readText(Charsets.UTF_8)
        val schema = TemplateSchema.fromJson(jsonContent)

        assertEquals("nhat-ky-cong-truong", schema.id)
        assertEquals(2, schema.maxPages)
        assertTrue(schema.noteCategories.contains("thi_cong"))
        assertTrue(schema.noteCategories.contains("nghiem_thu"))
        assertTrue(schema.noteCategories.contains("an_toan"))

        // Kiểm tra ngân sách từng trường
        val thiCongField = schema.summaryFields["cong_viec_thi_cong"]
        assertNotNull(thiCongField)
        assertEquals(15, thiCongField?.maxLines)
        assertEquals(100, thiCongField?.maxCharsPerLine)

        val nghiemThuField = schema.summaryFields["nghiem_thu"]
        assertNotNull(nghiemThuField)
        assertEquals(6, nghiemThuField?.maxLines)

        val anToanField = schema.summaryFields["an_toan"]
        assertNotNull(anToanField)
        assertEquals(3, anToanField?.maxLines)

        val yKienCdtField = schema.summaryFields["y_kien_cdt"]
        assertNotNull(yKienCdtField)
        assertEquals(200, yKienCdtField?.maxChars)
    }

    @Test
    fun testBilingualSchemaEnglishKeys() {
        val englishJson = """
            {
              "id": "custom_en_123",
              "name": "Daily Site Log",
              "max_pages": 3,
              "group_by": "ngay",
              "note_categories": ["thi_cong", "an_toan"],
              "summary_fields": [
                {
                  "key": "construction_work",
                  "type": "danh_sach",
                  "max_lines": 10,
                  "max_chars_per_line": 90,
                  "source": "thi_cong"
                },
                {
                  "key": "safety_notes",
                  "type": "van_ban",
                  "max_chars": 300,
                  "source": "an_toan"
                }
              ]
            }
        """.trimIndent()

        val schema = TemplateSchema.fromJson(englishJson)

        assertEquals("custom_en_123", schema.id)
        assertEquals("Daily Site Log", schema.name)
        assertEquals(3, schema.maxPages)
        assertEquals("ngay", schema.groupByType)
        assertEquals(2, schema.noteCategories.size)
        assertEquals(2, schema.summaryFields.size)

        val workField = schema.summaryFields["construction_work"]
        assertNotNull(workField)
        assertEquals("danh_sach", workField?.type)
        assertEquals(10, workField?.maxLines)
        assertEquals(90, workField?.maxCharsPerLine)

        val safetyField = schema.summaryFields["safety_notes"]
        assertNotNull(safetyField)
        assertEquals("van_ban", safetyField?.type)
        assertEquals(300, safetyField?.maxChars)
    }

    @Test
    fun testBilingualSchemaVietnameseKeys() {
        val vietnameseJson = """
            {
              "id": "custom_vn_123",
              "ten": "Nhật ký công trường",
              "gioi_han_trang": 2,
              "gom_theo": "ngay",
              "muc_ghi_chu": ["thi_cong"],
              "truong_tong_hop": {
                "cong_viec": {
                  "kieu": "danh_sach",
                  "toi_da_dong": 8,
                  "ky_tu_moi_dong": 80,
                  "muc_nguon": ["thi_cong"]
                }
              }
            }
        """.trimIndent()

        val schema = TemplateSchema.fromJson(vietnameseJson)

        assertEquals("custom_vn_123", schema.id)
        assertEquals("Nhật ký công trường", schema.name)
        assertEquals(2, schema.maxPages)
        assertEquals(1, schema.summaryFields.size)

        val field = schema.summaryFields["cong_viec"]
        assertNotNull(field)
        assertEquals("danh_sach", field?.type)
        assertEquals(8, field?.maxLines)
        assertEquals(80, field?.maxCharsPerLine)
    }

    @Test
    fun testFormSetupServiceGeneratedSchemaConforms() = runBlocking {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 0,
                endIndex = 10,
                truoc = "",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Thi công",
                loaiGoiY = "danh_sach",
                ma = "CONG_VIEC_THI_CONG",
                ten = "Công việc thi công",
                loai = "danh_sach",
                nguon = "thi_cong"
            ),
            FormCandidate(
                id = 2,
                startIndex = 20,
                endIndex = 30,
                truoc = "",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Ban chỉ huy",
                loaiGoiY = "so",
                ma = "BAN_CHI_HUY",
                ten = "Ban chỉ huy",
                loai = "so",
                nguon = "nhap_tay"
            )
        )

        val fakeInterceptor = Interceptor { chain ->
            val request = chain.request()
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"documentId": "mock_doc_id"}""".toResponseBody("application/json".toMediaType()))
                .build()
        }
        val client = OkHttpClient.Builder().addInterceptor(fakeInterceptor).build()
        val service = FormSetupService(httpClient = client)

        val result = service.applyPlaceholdersAndGenerateTemplate(
            workingDocId = "mock_doc_id",
            templateName = "Biểu mẫu thử nghiệm",
            candidates = candidates,
            accessToken = "mock_token"
        )

        val entity = result.getOrNull()
        assertNotNull(entity)
        val schemaJson = entity?.schemaJson.orEmpty()
        assertTrue(schemaJson.isNotBlank())

        // Phải parse thành công mà không ném ngoại lệ
        val schema = TemplateSchema.fromJson(schemaJson)
        assertEquals("Biểu mẫu thử nghiệm", schema.name)
        assertEquals(2, schema.maxPages)
        assertTrue(schema.summaryFields.containsKey("cong_viec_thi_cong"))
        assertTrue(schema.summaryFields.containsKey("ban_chi_huy"))
        assertEquals("danh_sach", schema.summaryFields["cong_viec_thi_cong"]?.type)
        assertEquals(8, schema.summaryFields["cong_viec_thi_cong"]?.maxLines)
        assertEquals(3, schema.summaryFields["ban_chi_huy"]?.maxLines)
    }

    @Test
    fun testResilientParsingNeverThrowsJSONException() {
        // Chuỗi rỗng
        val emptySchema = TemplateSchema.fromJson("{}")
        assertNotNull(emptySchema)
        assertEquals(2, emptySchema.maxPages)

        // Chuỗi hoàn toàn không phải JSON
        val malformedSchema = TemplateSchema.fromJson("NOT_A_JSON_STRING")
        assertNotNull(malformedSchema)
        assertEquals("fallback", malformedSchema.id)

        // JSON thiếu trường
        val partialSchema = TemplateSchema.fromJson("{\"id\": \"123\"}")
        assertNotNull(partialSchema)
        assertEquals("123", partialSchema.id)
        assertEquals(2, partialSchema.maxPages)
    }
}
