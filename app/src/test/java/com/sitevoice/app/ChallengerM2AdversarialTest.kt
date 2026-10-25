package com.sitevoice.app

import com.sitevoice.app.llm.ClassificationResult
import com.sitevoice.app.llm.HybridClassificationPipeline
import com.sitevoice.app.llm.NoteClassifier
import com.sitevoice.app.llm.TextNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengerM2AdversarialTest {

    // =========================================================================
    // 1. All 7 Homophones in Lowercase
    // =========================================================================

    @Test
    fun testAllSevenHomophonesLowercase() {
        assertEquals("cốp pha", TextNormalizer.normalize("cốc pha"))
        assertEquals("cốp pha", TextNormalizer.normalize("cốt pha"))
        assertEquals("cốp pha", TextNormalizer.normalize("coffa"))
        assertEquals("cốp pha", TextNormalizer.normalize("cop pha"))
        assertEquals("dầm sàn", TextNormalizer.normalize("rầm sàn"))
        assertEquals("nghiệm thu", TextNormalizer.normalize("nhiệm thu"))
        assertEquals("cốt thép", TextNormalizer.normalize("cốt phép"))
        assertEquals("giàn giáo", TextNormalizer.normalize("giàn dáo"))
        assertEquals("trát tường", TextNormalizer.normalize("chát tường"))
        assertEquals("bảo dưỡng", TextNormalizer.normalize("bảo dữ"))
    }

    // =========================================================================
    // 2. All 7 Homophones in Titlecase (Sentence Start)
    // =========================================================================

    @Test
    fun testAllSevenHomophonesTitlecase() {
        assertEquals("Cốp pha cột tầng 1", TextNormalizer.normalize("Cốc pha cột tầng 1"))
        assertEquals("Cốp pha dầm", TextNormalizer.normalize("Cốt pha dầm"))
        assertEquals("Cốp pha sàn", TextNormalizer.normalize("Coffa sàn"))
        assertEquals("Cốp pha móng", TextNormalizer.normalize("Cop pha móng"))
        assertEquals("Dầm sàn tầng 2", TextNormalizer.normalize("Rầm sàn tầng 2"))
        assertEquals("Nghiệm thu công việc", TextNormalizer.normalize("Nhiệm thu công việc"))
        assertEquals("Cốt thép đài móng", TextNormalizer.normalize("Cốt phép đài móng"))
        assertEquals("Giàn giáo bao che", TextNormalizer.normalize("Giàn dáo bao che"))
        assertEquals("Trát tường ngoài nhà", TextNormalizer.normalize("Chát tường ngoài nhà"))
        assertEquals("Bảo dưỡng bê tông", TextNormalizer.normalize("Bảo dữ bê tông"))
    }

    // =========================================================================
    // 3. All 7 Homophones in UPPERCASE
    // =========================================================================

    @Test
    fun testAllSevenHomophonesUppercase() {
        assertEquals("CỐP PHA", TextNormalizer.normalize("CỐC PHA"))
        assertEquals("CỐP PHA", TextNormalizer.normalize("CỐT PHA"))
        assertEquals("CỐP PHA", TextNormalizer.normalize("COFFA"))
        assertEquals("CỐP PHA", TextNormalizer.normalize("COP PHA"))
        assertEquals("DẦM SÀN", TextNormalizer.normalize("RẦM SÀN"))
        assertEquals("NGHIỆM THU", TextNormalizer.normalize("NHIỆM THU"))
        assertEquals("CỐT THÉP", TextNormalizer.normalize("CỐT PHÉP"))
        assertEquals("GIÀN GIÁO", TextNormalizer.normalize("GIÀN DÁO"))
        assertEquals("TRÁT TƯỜNG", TextNormalizer.normalize("CHÁT TƯỜNG"))
        assertEquals("BẢO DƯỠNG", TextNormalizer.normalize("BẢO DỮ"))
    }

    // =========================================================================
    // 4. Preservation of Numbers, Decimals, Axis Markers & Coordinates
    // =========================================================================

    @Test
    fun testPreserveNumbersAndAxisMarkers() {
        val input = "Nhiệm thu cốt phép rầm sàn trục A-B tầng 3, cao độ +11.20m, khối lượng 45.5 m3"
        val normalized = TextNormalizer.normalize(input)

        assertEquals(
            "Nghiệm thu cốt thép dầm sàn trục A-B tầng 3, cao độ +11.20m, khối lượng 45.5 m3",
            normalized
        )
        assertTrue(normalized.contains("trục A-B"))
        assertTrue(normalized.contains("tầng 3"))
        assertTrue(normalized.contains("+11.20m"))
        assertTrue(normalized.contains("45.5 m3"))
    }

    @Test
    fun testPreserveEngineeringCoordinatesAndRebarDiameters() {
        val input = "Kiểm tra coffa và cốt phép móng M1 trục 1-5 giao trục X1-X4, thép phi 18 và Ф20 a200, tọa độ X=543210.12, Y=1234567.89"
        val normalized = TextNormalizer.normalize(input)

        assertTrue(normalized.contains("cốp pha"))
        assertTrue(normalized.contains("cốt thép"))
        assertTrue(normalized.contains("trục 1-5 giao trục X1-X4"))
        assertTrue(normalized.contains("thép phi 18 và Ф20 a200"))
        assertTrue(normalized.contains("X=543210.12, Y=1234567.89"))
    }

    // =========================================================================
    // 5. Already Correct Terms Must NOT Be Corrupted
    // =========================================================================

    @Test
    fun testAlreadyCorrectTermsNotCorrupted() {
        val input = "Nghiệm thu cốp pha cốt thép dầm sàn, tháo giàn giáo để trát tường và bảo dưỡng bê tông"
        val normalized = TextNormalizer.normalize(input)
        assertEquals(input, normalized)
    }

    @Test
    fun testNonTargetHomophonesNotCorrupted() {
        // "cốt" in "cốt nền", "cốt cao độ", "cốt đai" must NOT be converted
        val inputCot = "Đo đạc cốt nền hoàn thiện +0.00 và kiểm tra cốt đai cột"
        val normalizedCot = TextNormalizer.normalize(inputCot)
        assertEquals(inputCot, normalizedCot)

        // "rầm rộ" must NOT be corrupted to "dầm rộ"
        val inputRamRo = "Công trường thi công rầm rộ tiến độ khẩn trương"
        val normalizedRamRo = TextNormalizer.normalize(inputRamRo)
        assertEquals(inputRamRo, normalizedRamRo)

        // "chát" alone must NOT be corrupted
        val inputChat = "Nước giếng khoan có vị chát chua"
        val normalizedChat = TextNormalizer.normalize(inputChat)
        assertEquals(inputChat, normalizedChat)

        // "đúc bê tông" must stay "đúc bê tông", not "đục bê tông"
        val inputDuc = "Đang đúc bê tông dầm sàn mác 300"
        val normalizedDuc = TextNormalizer.normalize(inputDuc)
        assertEquals(inputDuc, normalizedDuc)
    }

    // =========================================================================
    // 6. Stress Testing with Punctuation, Quotes & Parentheses
    // =========================================================================

    @Test
    fun testHomophonesWithPunctuationAndParentheses() {
        assertEquals("Đã xong (cốp pha), đang buộc (cốt thép).", TextNormalizer.normalize("Đã xong (cốc pha), đang buộc (cốt phép)."))
        assertEquals("Hạng mục: cốp pha; cốt thép; dầm sàn.", TextNormalizer.normalize("Hạng mục: cốt pha; cốt phép; rầm sàn."))
        assertEquals("Bắt đầu nghiệm thu: \"cốt thép\", \"cốp pha\"!", TextNormalizer.normalize("Bắt đầu nhiệm thu: \"cốt phép\", \"coffa\"!"))
        assertEquals("Hạ giàn giáo? Xong trát tường!", TextNormalizer.normalize("Hạ giàn dáo? Xong chát tường!"))
    }

    @Test
    fun testBaoDuPunctuationLookahead() {
        // Test bảo dữ with various trailing characters
        assertEquals("bảo dưỡng ẩm", TextNormalizer.normalize("bảo dữ ẩm"))
        assertEquals("bảo dưỡng.", TextNormalizer.normalize("bảo dữ."))
        assertEquals("bảo dưỡng,", TextNormalizer.normalize("bảo dữ,"))
        assertEquals("bảo dưỡng!", TextNormalizer.normalize("bảo dữ!"))
        assertEquals("bảo dưỡng?", TextNormalizer.normalize("bảo dữ?"))
        assertEquals("bảo dưỡng;", TextNormalizer.normalize("bảo dữ;"))
        assertEquals("bảo dưỡng:", TextNormalizer.normalize("bảo dữ:"))
    }

    // =========================================================================
    // 7. Full Hybrid Pipeline Propagation Stress Test
    // =========================================================================

    @Test
    fun testPipelineOfflinePropagationWithHomophones() = runBlocking {
        val pipeline = HybridClassificationPipeline(llmClassifier = null)
        val raw = "Sáng nhiệm thu cốc pha rầm sàn trục 2-4 tầng 2."
        val result = pipeline.classify(1L, raw)

        assertEquals("nghiem_thu", result.label)
        assertNotNull(result.correctedText)
        assertEquals("Sáng nghiệm thu cốp pha dầm sàn trục 2-4 tầng 2.", result.correctedText)
    }

    @Test
    fun testPipelinePreservesUserLabelAndCorrectsText() = runBlocking {
        val pipeline = HybridClassificationPipeline(llmClassifier = null)
        val raw = "cốt phép và giàn dáo"
        val result = pipeline.classify(2L, raw, userLabel = "thi_cong")

        assertEquals("thi_cong", result.label)
        assertEquals(1.0, result.confidence, 0.001)
        assertEquals("cốt thép và giàn giáo", result.correctedText)
    }
}
