package com.sitevoice.app

import com.sitevoice.app.llm.LlmClient
import com.sitevoice.app.llm.LlmMessage
import com.sitevoice.app.llm.NoteClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeLlmClient(
    private val responseContent: String
) : LlmClient {
    override suspend fun chat(
        messages: List<LlmMessage>,
        temperature: Float,
        responseJson: Boolean
    ): Result<String> {
        return Result.success(responseContent)
    }
}

class NoteClassifierTest {

    @Test
    fun testParseStandardJson() {
        val json = """{"ket_qua": [{"id": 12, "muc": "thi_cong", "do_chac_chan": 0.95}]}"""
        val classifier = NoteClassifier(FakeLlmClient(json))

        val result = classifier.parseClassificationJson(json, 12)
        assertEquals(12L, result.id)
        assertEquals("thi_cong", result.label)
        assertEquals(0.95, result.confidence, 0.001)
    }

    @Test
    fun testParseJsonWithMarkdownFences() {
        val json = """```json
{"ket_qua": [{"id": 15, "muc": "nghiem_thu", "do_chac_chan": 0.88}]}
```"""
        val classifier = NoteClassifier(FakeLlmClient(json))

        val result = classifier.parseClassificationJson(json, 15)
        assertEquals(15L, result.id)
        assertEquals("nghiem_thu", result.label)
        assertEquals(0.88, result.confidence, 0.001)
    }

    @Test
    fun testParseDirectObject() {
        val json = """{"muc": "an_toan", "do_chac_chan": 0.92}"""
        val classifier = NoteClassifier(FakeLlmClient(json))

        val result = classifier.parseClassificationJson(json, 99)
        assertEquals(99L, result.id)
        assertEquals("an_toan", result.label)
        assertEquals(0.92, result.confidence, 0.001)
    }

    @Test
    fun testParseUnknownLabelFallsBackToChuaPhanLoai() {
        val json = """{"muc": "khong_ro_rang_lam", "do_chac_chan": 0.5}"""
        val classifier = NoteClassifier(FakeLlmClient(json))

        val result = classifier.parseClassificationJson(json, 100)
        assertEquals("chua_phan_loai", result.label)
    }

    @Test
    fun testClassifyFullFlow() {
        runBlocking {
            val json = """{"ket_qua": [{"id": 7, "muc": "y_kien_tvgs", "do_chac_chan": 0.99}]}"""
            val classifier = NoteClassifier(FakeLlmClient(json))

            val res = classifier.classify(7, "Tư vấn giám sát yêu cầu bảo dưỡng bê tông đúng 7 ngày.")
            assertTrue("Classify should succeed", res.isSuccess)

            val parsed = res.getOrThrow()
            assertEquals("y_kien_tvgs", parsed.label)
            assertEquals(7L, parsed.id)
        }
    }

    @Test
    fun testParseJsonWithVanBanDaSuaStandardFormat() {
        val json = """{"ket_qua": [{"id": 12, "muc": "thi_cong", "do_chac_chan": 0.95, "van_ban_da_sua": "Đổ bê tông dầm sàn tầng 3 trục A-B"}]}"""
        val classifier = NoteClassifier(FakeLlmClient(json))

        val result = classifier.parseClassificationJson(json, 12)
        assertEquals(12L, result.id)
        assertEquals("thi_cong", result.label)
        assertEquals(0.95, result.confidence, 0.001)
        assertEquals("Đổ bê tông dầm sàn tầng 3 trục A-B", result.correctedText)
    }

    @Test
    fun testParseJsonWithVanBanDaSuaDirectObject() {
        val json = """{"id": 42, "muc": "nghiem_thu", "do_chac_chan": 0.93, "van_ban_da_sua": "Nghiệm thu cốp pha cốt thép dầm sàn"}"""
        val classifier = NoteClassifier(FakeLlmClient(json))

        val result = classifier.parseClassificationJson(json, 42)
        assertEquals(42L, result.id)
        assertEquals("nghiem_thu", result.label)
        assertEquals(0.93, result.confidence, 0.001)
        assertEquals("Nghiệm thu cốp pha cốt thép dầm sàn", result.correctedText)
    }

    @Test
    fun testParseJsonWithVanBanDaSuaMarkdownFence() {
        val json = """```json
{"ket_qua": [{"id": 15, "muc": "an_toan", "do_chac_chan": 0.88, "van_ban_da_sua": "Nhắc nhở công nhân dựng giàn giáo chắc chắn"}]}
```"""
        val classifier = NoteClassifier(FakeLlmClient(json))

        val result = classifier.parseClassificationJson(json, 15)
        assertEquals(15L, result.id)
        assertEquals("an_toan", result.label)
        assertEquals(0.88, result.confidence, 0.001)
        assertEquals("Nhắc nhở công nhân dựng giàn giáo chắc chắn", result.correctedText)
    }

    @Test
    fun testParseJsonWithoutVanBanDaSuaReturnsNull() {
        val json = """{"ket_qua": [{"id": 12, "muc": "thi_cong", "do_chac_chan": 0.95}]}"""
        val classifier = NoteClassifier(FakeLlmClient(json))

        val result = classifier.parseClassificationJson(json, 12)
        assertEquals(null, result.correctedText)
    }

    @Test
    fun testHybridPipelineReturnsCorrectedTextFromLlm() = runBlocking {
        val json = """{"ket_qua": [{"id": 101, "muc": "thi_cong", "do_chac_chan": 0.95, "van_ban_da_sua": "Xử lý hoàn thiện khu vực thang bộ trục 2-5 tầng 3"}]}"""
        val classifier = NoteClassifier(FakeLlmClient(json))
        val pipeline = com.sitevoice.app.llm.HybridClassificationPipeline(classifier)

        val result = pipeline.classify(101L, "xử lý hoàn thiện khu vực thang bộ trục 2-5 tầng 3")
        assertEquals(101L, result.id)
        assertEquals("thi_cong", result.label)
        assertEquals("Xử lý hoàn thiện khu vực thang bộ trục 2-5 tầng 3", result.correctedText)
    }

    @Test
    fun testHybridPipelineReturnsNormalizedTextOnOfflineRuleMatch() = runBlocking {
        val pipeline = com.sitevoice.app.llm.HybridClassificationPipeline(llmClassifier = null)

        val result = pipeline.classify(102L, "Nhiệm thu cốc pha và cốt phép rầm sàn tầng 2")
        assertEquals(102L, result.id)
        assertEquals("nghiem_thu", result.label)
        // CorrectedText must be normalized by TextNormalizer
        assertEquals("Nghiệm thu cốp pha và cốt thép dầm sàn tầng 2", result.correctedText)
    }
}
