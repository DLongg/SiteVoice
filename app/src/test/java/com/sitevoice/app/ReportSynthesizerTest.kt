package com.sitevoice.app

import com.sitevoice.app.data.NoteEntity
import com.sitevoice.app.llm.ReportSynthesizer
import com.sitevoice.app.template.TemplateSchema
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

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

            val fakeLlm = FakeLlmClient(mockResponse)
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

            val fakeLlm = FakeLlmClient(mockResponse)
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
        }
    }
}
