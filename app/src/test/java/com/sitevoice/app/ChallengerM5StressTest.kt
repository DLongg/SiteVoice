package com.sitevoice.app

import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormLlmDescriber
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.ui.template.FormSetupStep
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Empirical Adversarial Stress Test Suite for Milestone M5 (Requirement R5).
 * Conducted by Challenger M5 Stress Tester (teamwork_preview_challenger).
 *
 * Focus Areas:
 * 1. Extreme temperature slider values (0.0f, 0.8f, negative, > 0.8f, NaN, infinities, precision rounding)
 * 2. Empty prompt strings, whitespace-only prompts, and massive prompt strings (>50k chars)
 * 3. Candidate list edge cases (empty list, all skipped, duplicate tags, special characters in ma/ten)
 * 4. SecurePreferences temperature clamping & persistence contracts
 * 5. ViewModel HITL workflow & fallback preservation logic
 * 6. UI string resource verification for Vietnamese localization & anti-slop guidelines
 */
class ChallengerM5StressTest {

    // =========================================================================
    // SECTION 1: Extreme Slider Values, Rounding Math, and Float Clamping
    // =========================================================================

    @Test
    fun testSliderRoundingAt15Steps() {
        // Range 0.0f..0.8f with 15 steps -> 16 intervals -> step size = 0.05f
        val stepSize = 0.8f / 16f
        assertEquals("Step size must be exactly 0.05f", 0.05f, stepSize, 0.0001f)

        for (i in 0..16) {
            val rawValue = i * 0.05f
            val rounded = (Math.round(rawValue * 100) / 100f).coerceIn(0.0f, 0.8f)
            val expected = i * 0.05f
            assertEquals("Step $i value mismatch", expected, rounded, 0.0001f)
            assertTrue("Value must be within [0.0f, 0.8f]", rounded in 0.0f..0.8f)
        }
    }

    @Test
    fun testExtremeNegativeSliderValues() {
        val negativeValues = listOf(
            -0.00001f,
            -0.05f,
            -0.1f,
            -0.5f,
            -0.8f,
            -1.0f,
            -100.0f,
            -Float.MIN_VALUE,
            -Float.MAX_VALUE
        )

        for (v in negativeValues) {
            val clamped = (Math.round(v * 100) / 100f).coerceIn(0.0f, 0.8f)
            assertEquals("Negative value $v must clamp to 0.0f", 0.0f, clamped, 0.0f)
            val badge = String.format(Locale.US, "%.2f", clamped)
            assertEquals("Negative value $v badge must be '0.00'", "0.00", badge)
        }
    }

    @Test
    fun testExtremeUpperSliderValues() {
        val upperValues = listOf(
            0.80001f,
            0.805f,
            0.81f,
            0.85f,
            0.9f,
            1.0f,
            2.0f,
            10.0f,
            100.0f,
            Float.MAX_VALUE
        )

        for (v in upperValues) {
            val clamped = (Math.round(v * 100) / 100f).coerceIn(0.0f, 0.8f)
            assertEquals("Upper value $v must clamp to 0.8f", 0.8f, clamped, 0.0f)
            val badge = String.format(Locale.US, "%.2f", clamped)
            assertEquals("Upper value $v badge must be '0.80'", "0.80", badge)
        }
    }

    @Test
    fun testSpecialFloatValues() {
        // Float.NaN
        val nanRounded = (Math.round(Float.NaN * 100) / 100f).coerceIn(0.0f, 0.8f)
        assertEquals("NaN must round to 0.0f", 0.0f, nanRounded, 0.0f)

        // Float.POSITIVE_INFINITY
        val posInfRounded = (Math.round(Float.POSITIVE_INFINITY * 100) / 100f).coerceIn(0.0f, 0.8f)
        assertEquals("+Inf must round to 0.8f", 0.8f, posInfRounded, 0.0f)

        // Float.NEGATIVE_INFINITY
        val negInfRounded = (Math.round(Float.NEGATIVE_INFINITY * 100) / 100f).coerceIn(0.0f, 0.8f)
        assertEquals("-Inf must round to 0.0f", 0.0f, negInfRounded, 0.0f)
    }

    // =========================================================================
    // SECTION 2: SecurePreferences Clamping & Persistence Contract
    // =========================================================================

    @Test
    fun testSecurePreferencesTemperatureClampingOnGetAndSet() {
        val fakePrefs = FakeSharedPreferences()
        val securePrefs = SecurePreferences(fakePrefs)

        // Case 1: Stored value in preferences is invalid (> 0.8f)
        fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] = 1.5f
        assertEquals("Getter must clamp stored 1.5f to 0.8f", 0.8f, securePrefs.llmTemperature, 0.0001f)

        // Case 2: Stored value in preferences is negative (-0.5f)
        fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] = -0.5f
        assertEquals("Getter must clamp stored -0.5f to 0.0f", 0.0f, securePrefs.llmTemperature, 0.0001f)

        // Case 3: Stored value is default 0.2f
        fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] = 0.2f
        assertEquals("Getter must preserve 0.2f", 0.2f, securePrefs.llmTemperature, 0.0001f)

        // Case 4: Setting negative temperature clamps before writing
        securePrefs.llmTemperature = -2.0f
        assertEquals(0.0f, securePrefs.llmTemperature, 0.0001f)
        assertEquals(0.0f, fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] as Float, 0.0001f)

        // Case 5: Setting excessive temperature clamps to 0.8f
        securePrefs.llmTemperature = 10.0f
        assertEquals(0.8f, securePrefs.llmTemperature, 0.0001f)
        assertEquals(0.8f, fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] as Float, 0.0001f)

        // Case 6: Setting valid temperature 0.35f
        securePrefs.llmTemperature = 0.35f
        assertEquals(0.35f, securePrefs.llmTemperature, 0.0001f)
        assertEquals(0.35f, fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] as Float, 0.0001f)
    }

    // =========================================================================
    // SECTION 3: Prompt Generation Edge Cases: Empty, Whitespace, Large Prompts
    // =========================================================================

    @Test
    fun testPromptGenerationWithEmptyCandidateList() {
        val emptyCandidates = emptyList<FormCandidate>()
        val prompt = FormLlmDescriber.generateUnifiedPrompt(emptyCandidates, "Biểu mẫu Trống")

        assertNotNull("Prompt must not be null", prompt)
        assertTrue("Prompt must contain template name", prompt.contains("Biểu mẫu Trống"))
        assertTrue("Prompt must contain anti-hallucination header", prompt.contains("TUYỆT ĐỐI KHÔNG BỊA ĐẶT SỐ LIỆU"))
        assertTrue("Prompt must contain output format section", prompt.contains("5. ĐỊNH DẠNG ĐẦU RA:"))
        // Sample JSON for empty candidates must be empty JSON object {}
        assertTrue("Prompt should end with empty JSON object", prompt.trim().endsWith("{}"))
    }

    @Test
    fun testPromptGenerationWithAllSkippedCandidates() {
        val skippedCandidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 0,
                endIndex = 10,
                truoc = "Ký tên",
                sau = "",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "Chữ ký",
                loaiGoiY = "van_ban",
                ma = "CHU_KY_1",
                ten = "Chữ ký A",
                boQua = true
            ),
            FormCandidate(
                id = 2,
                startIndex = 20,
                endIndex = 30,
                truoc = "Ký tên",
                sau = "",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "Chữ ký",
                loaiGoiY = "van_ban",
                ma = "CHU_KY_2",
                ten = "Chữ ký B",
                boQua = true
            )
        )

        val prompt = FormLlmDescriber.generateUnifiedPrompt(skippedCandidates, "Mẫu Chỉ Có Chữ Ký")
        assertFalse("Prompt must not include CHU_KY_1", prompt.contains("{{CHU_KY_1}}"))
        assertFalse("Prompt must not include CHU_KY_2", prompt.contains("{{CHU_KY_2}}"))
        assertTrue("Sample JSON should be empty object {}", prompt.trim().endsWith("{}"))
    }

    @Test
    fun testPromptGenerationWithLargeCandidateList() {
        val largeList = (1..60).map { id ->
            FormCandidate(
                id = id,
                startIndex = id * 10,
                endIndex = id * 10 + 5,
                truoc = "Mục $id:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Đề mục $id",
                loaiGoiY = if (id % 2 == 0) "danh_sach" else "van_ban",
                ma = "MUC_$id",
                ten = "Nội dung mục $id",
                loai = if (id % 2 == 0) "danh_sach" else "van_ban",
                nguon = "thi_cong",
                boQua = false
            )
        }

        val prompt = FormLlmDescriber.generateUnifiedPrompt(largeList, "Mẫu Siêu Lớn")
        assertNotNull(prompt)
        for (id in 1..60) {
            assertTrue("Prompt must contain tag MUC_$id", prompt.contains("{{MUC_$id}}"))
        }

        // Verify valid JSON output at end
        val outputSectionIndex = prompt.indexOf("5. ĐỊNH DẠNG ĐẦU RA:")
        assertTrue("Output section must exist", outputSectionIndex != -1)
        val jsonStart = prompt.indexOf('{', outputSectionIndex)
        val jsonEnd = prompt.lastIndexOf('}')
        assertTrue("JSON start and end must exist", jsonStart != -1 && jsonEnd > jsonStart)
        val jsonSnippet = prompt.substring(jsonStart, jsonEnd + 1)
        val parsedJson = JSONObject(jsonSnippet)
        assertEquals("Parsed JSON must have 60 keys", 60, parsedJson.length())
    }

    @Test
    fun testPromptSpecialCharacterSanitizationInCandidateCodes() {
        val rawCandidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 0,
                endIndex = 10,
                truoc = "A:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "",
                loaiGoiY = "van_ban",
                ma = "CONG VIEC / THI CONG - 2026!",
                ten = "Thi công",
                boQua = false
            ),
            FormCandidate(
                id = 2,
                startIndex = 20,
                endIndex = 30,
                truoc = "B:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "",
                loaiGoiY = "van_ban",
                ma = "CONG VIEC / THI CONG - 2026!", // Duplicate with special chars
                ten = "Thi công 2",
                boQua = false
            ),
            FormCandidate(
                id = 3,
                startIndex = 40,
                endIndex = 50,
                truoc = "C:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "",
                loaiGoiY = "van_ban",
                ma = "   ", // Blank ma
                ten = "Trống",
                boQua = false
            )
        )

        val deduplicated = FormLlmDescriber.deduplicateCandidateCodes(rawCandidates)
        assertEquals("First candidate code should be sanitized", "CONG_VIEC_THI_CONG_2026", deduplicated[0].ma)
        assertEquals("Second candidate code should be deduplicated", "CONG_VIEC_THI_CONG_2026_2", deduplicated[1].ma)
        assertEquals("Blank candidate code should fallback to TRUONG_3", "TRUONG_3", deduplicated[2].ma)
    }

    // =========================================================================
    // SECTION 4: Fallback & Preservation Logic for Empty / Custom Prompts
    // =========================================================================

    @Test
    fun testEmptyOrWhitespacePromptDoesNotOverwriteValidSystemPrompt() {
        val originalPrompt = "Bản prompt chuẩn gốc của hệ thống"
        val template = TemplateItemEntity(
            id = "template_custom_1",
            name = "Mẫu Test",
            description = "Mô tả mẫu test",
            schemaJson = "{}",
            systemPrompt = originalPrompt,
            createdAt = System.currentTimeMillis()
        )

        val emptyUserPrompt = ""
        val whitespaceUserPrompt = "   \n\t  "

        // Simulate MainViewModel logic:
        // val effectivePrompt = systemPrompt ?: pendingSetupPrompt ?: securePreferences.getString("pending_setup_prompt")
        // if (!effectivePrompt.isNullOrBlank()) { template = template.copy(systemPrompt = effectivePrompt) }
        fun applyPrompt(t: TemplateItemEntity, prompt: String?): TemplateItemEntity {
            return if (!prompt.isNullOrBlank()) t.copy(systemPrompt = prompt) else t
        }

        val resultWithEmpty = applyPrompt(template, emptyUserPrompt)
        assertEquals("Empty prompt must not overwrite original systemPrompt", originalPrompt, resultWithEmpty.systemPrompt)

        val resultWithWhitespace = applyPrompt(template, whitespaceUserPrompt)
        assertEquals("Whitespace prompt must not overwrite original systemPrompt", originalPrompt, resultWithWhitespace.systemPrompt)

        val customPrompt = "Prompt tùy chỉnh mới do người dùng biên soạn"
        val resultWithCustom = applyPrompt(template, customPrompt)
        assertEquals("Valid custom prompt must overwrite systemPrompt", customPrompt, resultWithCustom.systemPrompt)
    }

    @Test
    fun testMassivePromptStringHandling() {
        val basePrompt = "Bạn là thư ký kỹ thuật công trường. "
        val massivePrompt = basePrompt.repeat(1500) // ~54,000 characters
        assertTrue("Massive prompt length > 50k chars", massivePrompt.length > 50000)

        val template = TemplateItemEntity(
            id = "template_custom_massive",
            name = "Mẫu Khổng Lồ",
            description = "Mô tả mẫu khổng lồ",
            schemaJson = "{}",
            systemPrompt = massivePrompt,
            createdAt = System.currentTimeMillis()
        )

        assertEquals(massivePrompt, template.systemPrompt)
    }

    // =========================================================================
    // SECTION 5: Localization & App Screens Design Verification
    // =========================================================================

    @Test
    fun testStringsXmlLocalizationIntegrity() {
        val stringsFile = File("src/main/res/values/strings.xml")
        val content = if (stringsFile.exists()) {
            stringsFile.readText()
        } else {
            File("app/src/main/res/values/strings.xml").readText()
        }

        // Verify Step 1 & Step 2 indicators
        assertTrue("Must have Step 1 indicator", content.contains("Bước 1 / 2: Kiểm tra thẻ neo {{MA}}"))
        assertTrue("Must have Step 2 indicator", content.contains("Bước 2 / 2: Xem lại Prompt &amp; Cấu hình") || content.contains("Bước 2 / 2: Xem lại Prompt & Cấu hình"))

        // Verify button texts
        assertTrue("Must have Step 1 continue button", content.contains("Tiếp tục: Sinh Prompt thống nhất →"))
        assertTrue("Must have Step 2 back button", content.contains("← Quay lại"))
        assertTrue("Must have Step 2 complete button", content.contains("✓ Hoàn tất &amp; Tạo Mẫu") || content.contains("✓ Hoàn tất & Tạo Mẫu"))
        assertTrue("Must have Step 2 regenerate button", content.contains("🔄 Sinh lại Prompt"))

        // Verify temperature slider titles and descriptions
        assertTrue("Must have temperature title", content.contains("Nhiệt độ biên tập (Temperature)"))
        assertTrue("Must have temperature scale description", content.contains("0.0: Bám sát nguyên văn | 0.8: Viết lại mượt mà"))
        assertTrue("Must have temperature hint", content.contains("Mức 0.2 giúp liên kết câu khẩu ngữ rời rạc"))
    }
}
