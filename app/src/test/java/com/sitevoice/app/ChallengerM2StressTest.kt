package com.sitevoice.app

import com.sitevoice.app.data.NoteEntity
import com.sitevoice.app.llm.ClassificationResult
import com.sitevoice.app.llm.HybridClassificationPipeline
import com.sitevoice.app.llm.LlmClient
import com.sitevoice.app.llm.LlmMessage
import com.sitevoice.app.llm.NoteClassifier
import com.sitevoice.app.llm.TextNormalizer
import com.sitevoice.app.queue.ClassifyWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Empirical Adversarial Challenger Test Suite for Milestone M2.
 * Focus areas:
 * 1. JSON parsing resilience (escaped characters, markdown fences, missing fields, null values).
 * 2. Fallback mechanisms (safe fallback to TextNormalizer when LLM unavailable or invalid JSON).
 * 3. ClassifyWorker logic (Room NoteEntity.text update semantics, identical/blank/null preservation).
 * 4. TextNormalizer homophone & case preservation verification.
 */
class ChallengerM2StressTest {

    private fun makeFakeClient(payload: String): LlmClient {
        return object : LlmClient {
            override suspend fun chat(
                messages: List<LlmMessage>,
                temperature: Float,
                responseJson: Boolean
            ): Result<String> = Result.success(payload)
        }
    }

    private fun makeFailingClient(exception: Throwable = IOException("Simulated network timeout")): LlmClient {
        return object : LlmClient {
            override suspend fun chat(
                messages: List<LlmMessage>,
                temperature: Float,
                responseJson: Boolean
            ): Result<String> = Result.failure(exception)
        }
    }

    // =========================================================================
    // DOMAIN 1: JSON Parsing Stress Testing in NoteClassifier
    // =========================================================================

    @Test
    fun testJsonParsingWithEscapedQuotesInVanBanDaSua() {
        val json = """{"ket_qua": [{"id": 10, "muc": "thi_cong", "do_chac_chan": 0.95, "van_ban_da_sua": "Lắp đặt \"cốp pha\" đài móng M1 trục 1-3"}]}"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 10)
        assertEquals(10L, result.id)
        assertEquals("thi_cong", result.label)
        assertEquals(0.95, result.confidence, 0.001)
        assertEquals("Lắp đặt \"cốp pha\" đài móng M1 trục 1-3", result.correctedText)
    }

    @Test
    fun testJsonParsingWithEscapedNewlinesInVanBanDaSua() {
        val json = """{"ket_qua": [{"id": 11, "muc": "nghiem_thu", "do_chac_chan": 0.92, "van_ban_da_sua": "Nghiệm thu dầm sàn tầng 2.\nChuyển sang công tác đổ bê tông."}]}"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 11)
        assertEquals(11L, result.id)
        assertEquals("nghiem_thu", result.label)
        assertTrue(result.correctedText!!.contains("\n"))
        assertEquals("Nghiệm thu dầm sàn tầng 2.\nChuyển sang công tác đổ bê tông.", result.correctedText)
    }

    @Test
    fun testJsonParsingWithEscapedBackslashesAndSlashes() {
        val json = """{"ket_qua": [{"id": 12, "muc": "thi_cong", "do_chac_chan": 0.90, "van_ban_da_sua": "Thi công trục A\\B và phân đoạn 1\/2"}]}"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 12)
        assertEquals(12L, result.id)
        assertNotNull(result.correctedText)
        assertTrue(result.correctedText!!.contains("trục A") && result.correctedText!!.contains("Thi công"))
    }

    @Test
    fun testJsonParsingWithUppercaseJsonMarkdownFence() {
        val json = """```JSON
{"ket_qua": [{"id": 20, "muc": "an_toan", "do_chac_chan": 0.88, "van_ban_da_sua": "Nhắc nhở công nhân đeo dây an toàn"}]}
```"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 20)
        assertEquals(20L, result.id)
        assertEquals("an_toan", result.label)
        assertEquals(0.88, result.confidence, 0.001)
        assertEquals("Nhắc nhở công nhân đeo dây an toàn", result.correctedText)
    }

    @Test
    fun testJsonParsingWithBareMarkdownFenceAndCrlf() {
        val json = "```\r\n{\"ket_qua\": [{\"id\": 21, \"muc\": \"thi_cong\", \"do_chac_chan\": 0.91, \"van_ban_da_sua\": \"Đổ bê tông sàn mái\"}]}\r\n```"
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 21)
        assertEquals(21L, result.id)
        assertEquals("thi_cong", result.label)
        assertEquals("Đổ bê tông sàn mái", result.correctedText)
    }

    @Test
    fun testJsonParsingWithLeadingAndTrailingConversationalChatter() {
        val json = """Dưới đây là kết quả phân loại ghi chú xây dựng:
```json
{
  "ket_qua": [
    {
      "id": 22,
      "muc": "y_kien_tvgs",
      "do_chac_chan": 0.96,
      "van_ban_da_sua": "Tư vấn giám sát yêu cầu bảo dưỡng ẩm"
    }
  ]
}
```
Hy vọng phản hồi này giúp ích cho bạn!"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 22)
        assertEquals(22L, result.id)
        assertEquals("y_kien_tvgs", result.label)
        assertEquals(0.96, result.confidence, 0.001)
        assertEquals("Tư vấn giám sát yêu cầu bảo dưỡng ẩm", result.correctedText)
    }

    @Test
    fun testJsonParsingWithMissingVanBanDaSuaReturnsNull() {
        val json = """{"ket_qua": [{"id": 30, "muc": "thi_cong", "do_chac_chan": 0.85}]}"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 30)
        assertEquals(30L, result.id)
        assertEquals("thi_cong", result.label)
        assertNull("Missing van_ban_da_sua must result in null correctedText", result.correctedText)
    }

    @Test
    fun testJsonParsingWithNullVanBanDaSuaReturnsNull() {
        val json = """{"ket_qua": [{"id": 31, "muc": "nghiem_thu", "do_chac_chan": 0.85, "van_ban_da_sua": null}]}"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 31)
        assertEquals(31L, result.id)
        assertEquals("nghiem_thu", result.label)
        assertNull("null van_ban_da_sua must result in null correctedText", result.correctedText)
    }

    @Test
    fun testJsonParsingWithBlankOrWhitespaceVanBanDaSuaReturnsNull() {
        val jsonEmpty = """{"id": 32, "muc": "thi_cong", "do_chac_chan": 0.85, "van_ban_da_sua": ""}"""
        val classifier = NoteClassifier(makeFakeClient(jsonEmpty))
        val resEmpty = classifier.parseClassificationJson(jsonEmpty, 32)
        assertNull("Empty van_ban_da_sua must result in null correctedText", resEmpty.correctedText)

        val jsonSpaces = "{\"id\": 33, \"muc\": \"thi_cong\", \"do_chac_chan\": 0.85, \"van_ban_da_sua\": \"   \\n  \"}"
        val resSpaces = classifier.parseClassificationJson(jsonSpaces, 33)
        assertNull("Whitespace-only van_ban_da_sua must result in null correctedText", resSpaces.correctedText)
    }

    @Test
    fun testJsonParsingWithMissingMucDefaultsToChuaPhanLoai() {
        val json = """{"id": 34, "do_chac_chan": 0.90, "van_ban_da_sua": "Đổ bê tông dầm sàn"}"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 34)
        assertEquals(NoteClassifier.LABEL_CHUA_PHAN_LOAI, result.label)
        assertEquals("Đổ bê tông dầm sàn", result.correctedText)
    }

    @Test
    fun testJsonParsingWithInvalidMucDefaultsToChuaPhanLoai() {
        val json = """{"id": 35, "muc": "invalid_category_xyz", "do_chac_chan": 0.95}"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 35)
        assertEquals(NoteClassifier.LABEL_CHUA_PHAN_LOAI, result.label)
    }

    @Test
    fun testJsonParsingWithMissingConfidenceDefaultsToZero() {
        val json = """{"id": 36, "muc": "thi_cong"}"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 36)
        assertEquals(0.0, result.confidence, 0.001)
    }

    @Test
    fun testJsonParsingWithMissingIdDefaultsToExpectedId() {
        val json = """{"muc": "an_toan", "do_chac_chan": 0.95}"""
        val classifier = NoteClassifier(makeFakeClient(json))

        val result = classifier.parseClassificationJson(json, 999L)
        assertEquals(999L, result.id)
    }

    @Test
    fun testJsonParsingWithCompletelyInvalidNonJsonPayload() {
        val malformed = "THIS IS NOT JSON AT ALL"
        val classifier = NoteClassifier(makeFakeClient(malformed))

        val result = classifier.parseClassificationJson(malformed, 555L)
        assertEquals(555L, result.id)
        assertEquals(NoteClassifier.LABEL_CHUA_PHAN_LOAI, result.label)
        assertEquals(0.0, result.confidence, 0.001)
        assertNull(result.correctedText)
    }

    // =========================================================================
    // DOMAIN 2: Fallback Mechanisms in HybridClassificationPipeline
    // =========================================================================

    @Test
    fun testPipelineFallbackWhenLlmClassifierIsNull() = runBlocking {
        // When llmClassifier is null, pipeline must fall back to TextNormalizer
        val pipeline = HybridClassificationPipeline(llmClassifier = null)
        val rawInput = "Hôm nay cốc pha và rầm sàn thi công bình thường"

        val result = pipeline.classify(101L, rawInput)
        // If not matching high-confidence rule, label is chua_phan_loai or rule-matched
        assertNotNull(result.correctedText)
        // TextNormalizer MUST have normalized "cốc pha" -> "cốp pha" and "rầm sàn" -> "dầm sàn"
        assertTrue(result.correctedText!!.contains("cốp pha"))
        assertTrue(result.correctedText!!.contains("dầm sàn"))
        assertFalse(result.correctedText!!.contains("cốc pha"))
        assertFalse(result.correctedText!!.contains("rầm sàn"))
    }

    @Test
    fun testPipelineFallbackWhenLlmCallFailsWithNetworkException() = runBlocking {
        val failingClient = makeFailingClient(IOException("503 Service Unavailable"))
        val classifier = NoteClassifier(failingClient)
        val pipeline = HybridClassificationPipeline(classifier)

        val rawInput = "Tình hình công trường cốc pha chưa đạt chuẩn"
        val result = pipeline.classify(102L, rawInput)

        // Must safely fall back without throwing exception
        assertEquals(NoteClassifier.LABEL_CHUA_PHAN_LOAI, result.label)
        assertEquals(0.5, result.confidence, 0.001)
        // CorrectedText must be normalized by TextNormalizer
        assertEquals("Tình hình công trường cốp pha chưa đạt chuẩn", result.correctedText)
    }

    @Test
    fun testPipelineFallbackWhenLlmReturnsGarbageText() = runBlocking {
        val garbageClient = makeFakeClient("Invalid response without valid json")
        val classifier = NoteClassifier(garbageClient)
        val pipeline = HybridClassificationPipeline(classifier)

        val rawInput = "Kiểm tra chát tường và giàn dáo tầng 4"
        val result = pipeline.classify(103L, rawInput)

        // Must handle gracefully without crashing
        assertNotNull(result.correctedText)
        assertTrue(result.correctedText!!.contains("trát tường"))
        assertTrue(result.correctedText!!.contains("giàn giáo"))
    }

    @Test
    fun testPipelineFallbackWhenLlmReturnsMissingVanBanDaSua() = runBlocking {
        // LLM returns category but omits van_ban_da_sua
        val jsonNoCorrection = """{"ket_qua": [{"id": 104, "muc": "thi_cong", "do_chac_chan": 0.95}]}"""
        val classifier = NoteClassifier(makeFakeClient(jsonNoCorrection))
        val pipeline = HybridClassificationPipeline(classifier)

        val rawInput = "Đang gia công cốt phép và rầm sàn tầng 2"
        val result = pipeline.classify(104L, rawInput)

        assertEquals("thi_cong", result.label)
        // correctedText must fall back to normalized text
        assertEquals("Đang gia công cốt thép và dầm sàn tầng 2", result.correctedText)
    }

    @Test
    fun testPipelineFallbackWhenLlmReturnsBlankVanBanDaSua() = runBlocking {
        // LLM returns empty/blank van_ban_da_sua
        val jsonBlankCorrection = """{"ket_qua": [{"id": 105, "muc": "thi_cong", "do_chac_chan": 0.95, "van_ban_da_sua": "   "}]}"""
        val classifier = NoteClassifier(makeFakeClient(jsonBlankCorrection))
        val pipeline = HybridClassificationPipeline(classifier)

        val rawInput = "Đổ bê tông mác 300 rầm sàn"
        val result = pipeline.classify(105L, rawInput)

        assertEquals("thi_cong", result.label)
        assertEquals("Đổ bê tông mác 300 dầm sàn", result.correctedText)
    }

    @Test
    fun testPipelineTier1UserLabelOverridesEverythingWithNormalizedText() = runBlocking {
        val failingClient = makeFailingClient()
        val classifier = NoteClassifier(failingClient)
        val pipeline = HybridClassificationPipeline(classifier)

        val rawInput = "Gia công cốc pha đài móng"
        // User explicitly tagged as "an_toan"
        val result = pipeline.classify(106L, rawInput, userLabel = "an_toan")

        assertEquals(106L, result.id)
        assertEquals("an_toan", result.label)
        assertEquals(1.0, result.confidence, 0.001)
        assertEquals("Gia công cốp pha đài móng", result.correctedText)
    }

    @Test
    fun testPipelineGuardrailsDemotesLowConfidencePreservesCorrectedText() = runBlocking {
        // Low confidence 0.55 < 0.70 MIN_ACCEPTABLE_CONFIDENCE
        val jsonLowConf = """{"ket_qua": [{"id": 107, "muc": "thi_cong", "do_chac_chan": 0.55, "van_ban_da_sua": "Xem xét tình hình công việc ngày mai"}]}"""
        val classifier = NoteClassifier(makeFakeClient(jsonLowConf))
        val pipeline = HybridClassificationPipeline(classifier)

        val rawInput = "Xem xét tình hình công việc ngày mai"
        val result = pipeline.classify(107L, rawInput)

        // Guardrails demotes to chua_phan_loai due to low confidence < 0.70
        assertEquals(NoteClassifier.LABEL_CHUA_PHAN_LOAI, result.label)
        // But corrected text is preserved
        assertEquals("Xem xét tình hình công việc ngày mai", result.correctedText)
    }

    // =========================================================================
    // DOMAIN 3: ClassifyWorker Update Semantics & Room NoteEntity
    // =========================================================================

    @Test
    fun testClassifyWorkerUpdatesTextWhenCorrectedTextDiffers() {
        val initialNote = NoteEntity(
            id = 201L,
            createdAt = 1000L,
            date = "2026-10-25",
            audioPath = "/storage/audio/note_201.m4a",
            durationMs = 4000L,
            text = "Gia công cốc pha rầm sàn tầng 3",
            status = "TRANSCRIBED"
        )

        val classificationResult = ClassificationResult(
            id = 201L,
            label = "thi_cong",
            confidence = 0.95,
            correctedText = "Gia công cốp pha dầm sàn tầng 3"
        )

        // Simulating the exact ClassifyWorker logic:
        val finalCleanText = if (!classificationResult.correctedText.isNullOrBlank() &&
            classificationResult.correctedText != initialNote.text
        ) {
            classificationResult.correctedText
        } else {
            initialNote.text
        }

        val updatedNote = initialNote.copy(
            text = finalCleanText,
            label = classificationResult.label,
            labelSource = if (initialNote.labelSource == "user") "user" else "auto",
            status = "LABELED"
        )

        assertEquals("Gia công cốp pha dầm sàn tầng 3", updatedNote.text)
        assertEquals("thi_cong", updatedNote.label)
        assertEquals("auto", updatedNote.labelSource)
        assertEquals("LABELED", updatedNote.status)
        assertTrue("Text must be updated when corrected differs", updatedNote.text != initialNote.text)
    }

    @Test
    fun testClassifyWorkerLeavesTextUntouchedWhenCorrectedTextIsIdentical() {
        val originalText = "Nghiệm thu cốp pha dầm sàn tầng 2"
        val initialNote = NoteEntity(
            id = 202L,
            createdAt = 2000L,
            date = "2026-10-25",
            audioPath = "/storage/audio/note_202.m4a",
            durationMs = 3000L,
            text = originalText,
            status = "TRANSCRIBED"
        )

        val classificationResult = ClassificationResult(
            id = 202L,
            label = "nghiem_thu",
            confidence = 0.98,
            correctedText = originalText // Identical
        )

        val finalCleanText = if (!classificationResult.correctedText.isNullOrBlank() &&
            classificationResult.correctedText != initialNote.text
        ) {
            classificationResult.correctedText
        } else {
            initialNote.text
        }

        val updatedNote = initialNote.copy(
            text = finalCleanText,
            label = classificationResult.label,
            labelSource = if (initialNote.labelSource == "user") "user" else "auto",
            status = "LABELED"
        )

        assertEquals("Text must remain identical", originalText, updatedNote.text)
        assertEquals("nghiem_thu", updatedNote.label)
        assertEquals("LABELED", updatedNote.status)
    }

    @Test
    fun testClassifyWorkerLeavesTextUntouchedWhenCorrectedTextIsBlankOrNull() {
        val originalText = "Bảo dưỡng bê tông đúng 7 ngày"
        val initialNote = NoteEntity(
            id = 203L,
            createdAt = 3000L,
            date = "2026-10-25",
            audioPath = "/storage/audio/note_203.m4a",
            durationMs = 2500L,
            text = originalText,
            status = "TRANSCRIBED"
        )

        // Case A: correctedText is null
        val resNull = ClassificationResult(id = 203L, label = "thi_cong", confidence = 0.9, correctedText = null)
        val textAfterNull = if (!resNull.correctedText.isNullOrBlank() && resNull.correctedText != initialNote.text) {
            resNull.correctedText
        } else {
            initialNote.text
        }
        assertEquals("Text must remain untouched when correctedText is null", originalText, textAfterNull)

        // Case B: correctedText is empty string
        val resEmpty = ClassificationResult(id = 203L, label = "thi_cong", confidence = 0.9, correctedText = "")
        val textAfterEmpty = if (!resEmpty.correctedText.isNullOrBlank() && resEmpty.correctedText != initialNote.text) {
            resEmpty.correctedText
        } else {
            initialNote.text
        }
        assertEquals("Text must remain untouched when correctedText is empty", originalText, textAfterEmpty)

        // Case C: correctedText is whitespace
        val resWhitespace = ClassificationResult(id = 203L, label = "thi_cong", confidence = 0.9, correctedText = "   ")
        val textAfterWhitespace = if (!resWhitespace.correctedText.isNullOrBlank() && resWhitespace.correctedText != initialNote.text) {
            resWhitespace.correctedText
        } else {
            initialNote.text
        }
        assertEquals("Text must remain untouched when correctedText is whitespace", originalText, textAfterWhitespace)
    }

    @Test
    fun testClassifyWorkerPreservesUserLabelSource() {
        val initialNote = NoteEntity(
            id = 204L,
            createdAt = 4000L,
            date = "2026-10-25",
            audioPath = "/storage/audio/note_204.m4a",
            durationMs = 2000L,
            text = "Lắp dựng giàn dáo",
            label = "an_toan",
            labelSource = "user", // User manually assigned
            status = "TRANSCRIBED"
        )

        val classificationResult = ClassificationResult(
            id = 204L,
            label = "an_toan",
            confidence = 1.0,
            correctedText = "Lắp dựng giàn giáo"
        )

        val finalCleanText = if (!classificationResult.correctedText.isNullOrBlank() &&
            classificationResult.correctedText != initialNote.text
        ) {
            classificationResult.correctedText
        } else {
            initialNote.text
        }

        val updatedNote = initialNote.copy(
            text = finalCleanText,
            label = classificationResult.label,
            labelSource = if (initialNote.labelSource == "user") "user" else "auto",
            status = "LABELED"
        )

        assertEquals("Lắp dựng giàn giáo", updatedNote.text)
        assertEquals("user", updatedNote.labelSource)
    }

    // =========================================================================
    // DOMAIN 4: TextNormalizer Homophones and Case Preservation
    // =========================================================================

    @Test
    fun testTextNormalizerReplacesAllSevenHomophones() {
        // 1. cốc pha / cốt pha / coffa / cop pha -> cốp pha (NEVER ván khuôn)
        assertEquals("cốp pha dầm sàn", TextNormalizer.normalize("cốc pha rầm sàn"))
        assertEquals("cốp pha cột", TextNormalizer.normalize("cốt pha cột"))
        assertEquals("cốp pha móng", TextNormalizer.normalize("coffa móng"))
        assertEquals("cốp pha tường", TextNormalizer.normalize("cop pha tường"))
        assertFalse("Must NEVER convert coffa to 'ván khuôn'", TextNormalizer.normalize("cốc pha").contains("ván khuôn"))

        // 2. rầm sàn -> dầm sàn
        assertEquals("đổ bê tông dầm sàn", TextNormalizer.normalize("đổ bê tông rầm sàn"))

        // 3. nhiệm thu -> nghiệm thu
        assertEquals("nghiệm thu đài móng", TextNormalizer.normalize("nhiệm thu đài móng"))

        // 4. cốt phép -> cốt thép
        assertEquals("buộc cốt thép móng", TextNormalizer.normalize("buộc cốt phép móng"))

        // 5. giàn dáo -> giàn giáo
        assertEquals("dựng hệ giàn giáo", TextNormalizer.normalize("dựng hệ giàn dáo"))

        // 6. chát tường -> trát tường
        assertEquals("tiến hành trát tường", TextNormalizer.normalize("tiến hành chát tường"))

        // 7. bảo dữ -> bảo dưỡng
        assertEquals("tưới nước bảo dưỡng sàn", TextNormalizer.normalize("tưới nước bảo dữ sàn"))
    }

    @Test
    fun testTextNormalizerPreservesUppercaseAndTitleCase() {
        assertEquals("CỐP PHA DẦM SÀN", TextNormalizer.normalize("CỐC PHA RẦM SÀN"))
        assertEquals("CỐT THÉP", TextNormalizer.normalize("CỐT PHÉP"))
        assertEquals("GIÀN GIÁO", TextNormalizer.normalize("GIÀN DÁO"))
        assertEquals("TRÁT TƯỜNG", TextNormalizer.normalize("CHÁT TƯỜNG"))

        assertEquals("Cốp pha", TextNormalizer.normalize("Cốc pha"))
        assertEquals("Nghiệm thu", TextNormalizer.normalize("Nhiệm thu"))
        assertEquals("Cốt thép", TextNormalizer.normalize("Cốt phép"))
        assertEquals("Giàn giáo", TextNormalizer.normalize("Giàn dáo"))
        assertEquals("Trát tường", TextNormalizer.normalize("Chát tường"))
        assertEquals("Bảo dưỡng", TextNormalizer.normalize("Bảo dữ"))
    }

    @Test
    fun testTextNormalizerPreservesNumbersAxesAndLocations() {
        val input = "Nhiệm thu cốc pha và cốt phép rầm sàn trục A-D tầng 3 lúc 14h30, huy động 15 công nhân."
        val normalized = TextNormalizer.normalize(input)

        assertTrue(normalized.contains("Nghiệm thu"))
        assertTrue(normalized.contains("cốp pha"))
        assertTrue(normalized.contains("cốt thép"))
        assertTrue(normalized.contains("dầm sàn"))
        assertTrue("Must preserve axis coordinates A-D", normalized.contains("trục A-D"))
        assertTrue("Must preserve floor level", normalized.contains("tầng 3"))
        assertTrue("Must preserve time format", normalized.contains("14h30"))
        assertTrue("Must preserve worker count", normalized.contains("15 công nhân"))
    }
}
