package com.sitevoice.app

import com.sitevoice.app.data.NoteEntity
import com.sitevoice.app.llm.LlmClient
import com.sitevoice.app.llm.LlmMessage
import com.sitevoice.app.llm.ReportSynthesizer
import com.sitevoice.app.template.SummaryFieldConfig
import com.sitevoice.app.template.TemplateSchema
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class RecordingLlmClient(
    private val responseContent: String
) : LlmClient {
    var lastMessages: List<LlmMessage>? = null
    var lastTemperature: Float? = null
    var lastResponseJson: Boolean? = null
    var callCount: Int = 0

    override suspend fun chat(
        messages: List<LlmMessage>,
        temperature: Float,
        responseJson: Boolean
    ): Result<String> {
        lastMessages = messages
        lastTemperature = temperature
        lastResponseJson = responseJson
        callCount++
        return Result.success(responseContent)
    }
}

class ReportSynthesizerTest {

    private lateinit var schema: TemplateSchema

    @Before
    fun setUp() {
        val schemaFile = File("src/main/assets/templates/nhat-ky-cong-truong/schema.json")
        val altFile = File("app/src/main/assets/templates/nhat-ky-cong-truong/schema.json")
        val target = if (schemaFile.exists()) schemaFile else altFile
        schema = TemplateSchema.fromJson(target.readText(Charsets.UTF_8))
    }

    @Test
    fun testSynthesizeValidReport() {
        runBlocking {
            val mockResponse = """
            {
              "cong_viec_thi_cong": [
                "Đổ bê tông dầm sàn tầng 2 khối lượng 45m3 hoàn thành 16h00",
                "Lắp dựng cốt thép cột trục A-C"
              ],
              "nghiem_thu": [
                "Nghiệm thu ván khuôn dầm sàn tầng 2 đạt kỹ thuật"
              ],
              "an_toan": [
                "Kiểm tra hệ thống lan can an toàn khu vực thang bộ"
              ],
              "y_kien_cdt": "Đồng ý chuyển bước thi công.",
              "y_kien_tvgs": "Yêu cầu tưới nước bảo dưỡng đầy đủ.",
              "y_kien_nha_thau": "Cam kết bảo dưỡng theo đúng quy trình.",
              "chua_phan_loai": []
            }
            """

            val fakeLlm = RecordingLlmClient(mockResponse)
            val synthesizer = ReportSynthesizer(fakeLlm, schema)

            val notes = listOf(
                NoteEntity(
                    createdAt = 1000L,
                    date = "2026-10-20",
                    audioPath = "/audio/1.m4a",
                    durationMs = 5000,
                    text = "Đổ bê tông sàn tầng 2 45m3",
                    label = "thi_cong",
                    status = "LABELED"
                ),
                NoteEntity(
                    createdAt = 2000L,
                    date = "2026-10-20",
                    audioPath = "/audio/2.m4a",
                    durationMs = 4000,
                    text = "Nghiệm thu ván khuôn dầm sàn",
                    label = "nghiem_thu",
                    status = "LABELED"
                )
            )

            val result = synthesizer.synthesizeReport("2026-10-20", notes)
            assertTrue("Synthesize should succeed", result.isSuccess)

            val jsonStr = result.getOrThrow()
            val jsonObj = JSONObject(jsonStr)

            val thiCong = jsonObj.getJSONArray("cong_viec_thi_cong")
            assertEquals(2, thiCong.length())

            val nghiemThu = jsonObj.getJSONArray("nghiem_thu")
            assertEquals(1, nghiemThu.length())

            assertEquals("Đồng ý chuyển bước thi công.", jsonObj.getString("y_kien_cdt"))
        }
    }

    @Test
    fun testSynthesizePersonalJournalReport() {
        runBlocking {
            val personalSchemaFile = File("src/main/assets/templates/nhat-ky-ca-nhan/schema.json")
            val altFile = File("app/src/main/assets/templates/nhat-ky-ca-nhan/schema.json")
            val target = if (personalSchemaFile.exists()) personalSchemaFile else altFile
            val personalSchema = TemplateSchema.fromJson(target.readText(Charsets.UTF_8))

            val mockResponse = """
            {
              "cong_viec_hoan_thanh": [
                "Hoàn thành thiết kế Thư viện Mẫu cho app",
                "Chạy xong 29 unit tests pass 100%"
              ],
              "viec_ton_dong": [
                "Cập nhật tài liệu bàn giao buổi tối"
              ],
              "suy_nghi_bai_hoc": [
                "Tách nhỏ task giúp kiểm soát lỗi tốt hơn"
              ],
              "suc_khoe_tam_trang": "Tập trung và nhiều năng lượng",
              "chua_phan_loai": []
            }
            """

            val fakeLlm = RecordingLlmClient(mockResponse)
            val customPrompt = "Bạn là trợ lý nhật ký cá nhân..."
            val synthesizer = ReportSynthesizer(fakeLlm, personalSchema, customSystemPrompt = customPrompt)

            val notes = listOf(
                NoteEntity(
                    createdAt = 1000L,
                    date = "2026-10-20",
                    audioPath = "/audio/1.m4a",
                    durationMs = 5000,
                    text = "Hoàn thành code tính năng thư viện mẫu",
                    label = "cong_viec",
                    status = "LABELED"
                ),
                NoteEntity(
                    createdAt = 2000L,
                    date = "2026-10-20",
                    audioPath = "/audio/2.m4a",
                    durationMs = 4000,
                    text = "Nhớ cập nhật file SUMMARY tối nay",
                    label = "ton_dong",
                    status = "LABELED"
                )
            )

            val result = synthesizer.synthesizeReport("2026-10-20", notes)
            assertTrue("Synthesize personal journal should succeed", result.isSuccess)

            val jsonStr = result.getOrThrow()
            val jsonObj = JSONObject(jsonStr)

            val completed = jsonObj.getJSONArray("cong_viec_hoan_thanh")
            assertEquals(2, completed.length())

            val pending = jsonObj.getJSONArray("viec_ton_dong")
            assertEquals(1, pending.length())

            assertEquals("Tập trung và nhiều năng lượng", jsonObj.getString("suc_khoe_tam_trang"))

            // Verify userPrompt dynamically contains target fields of personal journal
            val userPrompt = fakeLlm.lastMessages?.find { it.role == "user" }?.content
            assertNotNull(userPrompt)
            val targetJsonBlock = userPrompt!!.substringAfter("Cấu trúc JSON mong muốn:")
            assertTrue(targetJsonBlock.contains("\"cong_viec_hoan_thanh\": []"))
            assertTrue(targetJsonBlock.contains("\"viec_ton_dong\": []"))
            assertTrue(targetJsonBlock.contains("\"suy_nghi_bai_hoc\": []"))
            assertTrue(targetJsonBlock.contains("\"suc_khoe_tam_trang\": \"\""))
        }
    }

    @Test
    fun testTemperaturePassedToLlmClient() {
        runBlocking {
            // 1. Default temperature is 0.2f
            val recorder1 = RecordingLlmClient("{}")
            val synthesizer1 = ReportSynthesizer(recorder1, schema)
            synthesizer1.synthesizeReport("2026-10-20", emptyList())
            assertEquals(0.2f, recorder1.lastTemperature ?: 0f, 0.0001f)

            // 2. Custom constructor temperature (e.g. 0.55f)
            val recorder2 = RecordingLlmClient("{}")
            val synthesizer2 = ReportSynthesizer(recorder2, schema, temperature = 0.55f)
            synthesizer2.synthesizeReport("2026-10-20", emptyList())
            assertEquals(0.55f, recorder2.lastTemperature ?: 0f, 0.0001f)

            // 3. Method parameter temperature override (e.g. 0.75f)
            val recorder3 = RecordingLlmClient("{}")
            val synthesizer3 = ReportSynthesizer(recorder3, schema, temperature = 0.2f)
            synthesizer3.synthesizeReport("2026-10-20", emptyList(), temperature = 0.75f)
            assertEquals(0.75f, recorder3.lastTemperature ?: 0f, 0.0001f)
        }
    }

    @Test
    fun testDynamicJsonPromptStructureReflectsActiveTemplateSummaryFields() {
        runBlocking {
            val customSchema = TemplateSchema(
                id = "custom_form_template",
                name = "Biểu mẫu tùy chỉnh",
                groupByType = "ngay",
                maxPages = 2,
                noteCategories = listOf("tien_do", "vat_tu", "chua_phan_loai"),
                summaryFields = mapOf(
                    "tien_do" to SummaryFieldConfig(type = "danh_sach", maxLines = 10),
                    "vat_tu_ve" to SummaryFieldConfig(type = "danh_sach", maxLines = 5),
                    "nhan_luc_chi_tiet" to SummaryFieldConfig(type = "van_ban", maxChars = 200),
                    "ngay" to SummaryFieldConfig(type = "so", sourceCategories = listOf("nhap_tay")),
                    "thoi_tiet_buoi_sang" to SummaryFieldConfig(type = "o_tich", sourceCategories = listOf("thoi_tiet"))
                )
            )

            val recorder = RecordingLlmClient("{}")
            val synthesizer = ReportSynthesizer(recorder, customSchema, temperature = 0.3f)
            synthesizer.synthesizeReport("2026-10-20", emptyList())

            assertEquals(0.3f, recorder.lastTemperature ?: 0f, 0.0001f)

            val userPrompt = recorder.lastMessages?.find { it.role == "user" }?.content
            assertNotNull(userPrompt)

            val targetJsonBlock = userPrompt!!.substringAfter("Cấu trúc JSON mong muốn:")
            // Verify active dynamic summary fields are included with matching types
            assertTrue("Expected tien_do list in target JSON", targetJsonBlock.contains("\"tien_do\": []"))
            assertTrue("Expected vat_tu_ve list in target JSON", targetJsonBlock.contains("\"vat_tu_ve\": []"))
            assertTrue("Expected nhan_luc_chi_tiet string in target JSON", targetJsonBlock.contains("\"nhan_luc_chi_tiet\": \"\""))

            // Verify manual source categories (nhap_tay, thoi_tiet) are excluded from the target JSON
            assertFalse("ngay (nhap_tay) should be excluded", targetJsonBlock.contains("\"ngay\""))
            assertFalse("thoi_tiet_buoi_sang (thoi_tiet) should be excluded", targetJsonBlock.contains("\"thoi_tiet_buoi_sang\""))
        }
    }

    @Test
    fun testDynamicJsonPromptStructureFallbackWhenSummaryFieldsEmpty() {
        runBlocking {
            val emptySchema = TemplateSchema(
                id = "custom_empty",
                name = "Biểu mẫu rỗng",
                groupByType = "ngay",
                maxPages = 1,
                noteCategories = emptyList(),
                summaryFields = emptyMap()
            )

            val recorder = RecordingLlmClient("{}")
            val synthesizer = ReportSynthesizer(recorder, emptySchema)
            synthesizer.synthesizeReport("2026-10-20", emptyList())

            val userPrompt = recorder.lastMessages?.find { it.role == "user" }?.content
            assertNotNull(userPrompt)

            val targetJsonBlock = userPrompt!!.substringAfter("Cấu trúc JSON mong muốn:")
            assertTrue("Expected standard fallback cong_viec_thi_cong", targetJsonBlock.contains("\"cong_viec_thi_cong\""))
            assertTrue("Expected standard fallback nghiem_thu", targetJsonBlock.contains("\"nghiem_thu\": []"))
            assertTrue("Expected standard fallback an_toan", targetJsonBlock.contains("\"an_toan\": []"))
            assertTrue("Expected standard fallback y_kien_cdt", targetJsonBlock.contains("\"y_kien_cdt\": \"\""))
            assertTrue("Expected standard fallback y_kien_tvgs", targetJsonBlock.contains("\"y_kien_tvgs\": \"\""))
            assertTrue("Expected standard fallback y_kien_nha_thau", targetJsonBlock.contains("\"y_kien_nha_thau\": \"\""))
            assertTrue("Expected standard fallback chua_phan_loai", targetJsonBlock.contains("\"chua_phan_loai\": []"))
        }
    }

    @Test
    fun testDynamicJsonPromptStructureFallbackWhenAllSummaryFieldsAreManual() {
        runBlocking {
            val allManualSchema = TemplateSchema(
                id = "custom_manual_only",
                name = "Toàn bộ nhập tay",
                groupByType = "ngay",
                maxPages = 1,
                noteCategories = emptyList(),
                summaryFields = mapOf(
                    "ngay" to SummaryFieldConfig(type = "so", sourceCategories = listOf("nhap_tay")),
                    "thoi_tiet" to SummaryFieldConfig(type = "o_tich", sourceCategories = listOf("thoi_tiet"))
                )
            )

            val recorder = RecordingLlmClient("{}")
            val synthesizer = ReportSynthesizer(recorder, allManualSchema)
            synthesizer.synthesizeReport("2026-10-20", emptyList())

            val userPrompt = recorder.lastMessages?.find { it.role == "user" }?.content
            assertNotNull(userPrompt)

            val targetJsonBlock = userPrompt!!.substringAfter("Cấu trúc JSON mong thông tin:")
                .ifBlank { userPrompt.substringAfter("Cấu trúc JSON mong muốn:") }
            assertTrue("Expected standard fallback cong_viec_thi_cong", targetJsonBlock.contains("\"cong_viec_thi_cong\""))
            assertTrue("Expected standard fallback nghiem_thu", targetJsonBlock.contains("\"nghiem_thu\": []"))
        }
    }
}
