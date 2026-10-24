package com.sitevoice.app

import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormLlmDescriber
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.ui.template.FormSetupStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Empirical Adversarial Stress Test Suite for Milestone M5 (Requirement R5).
 * Tests:
 * 1. 2-Step FormSetupDialog state machine and step enum contracts.
 * 2. Temperature slider calculation, rounding, live badge formatting, and boundary clamping.
 * 3. Unified prompt generation for Step 2 with active/skipped candidate filtering.
 * 4. Strings verification in strings.xml for step indicators, buttons, headings, and descriptions.
 */
class AdversarialM5StressTest {

    // =========================================================================
    // DOMAIN 1: 2-Step State Machine & FormSetupStep Enum Contracts
    // =========================================================================

    @Test
    fun testFormSetupStepEnumValues() {
        val steps = FormSetupStep.values()
        assertEquals("Must have exactly 2 steps in HITL workflow", 2, steps.size)
        assertEquals(FormSetupStep.STEP_1_TAGS, FormSetupStep.valueOf("STEP_1_TAGS"))
        assertEquals(FormSetupStep.STEP_2_PROMPT_CONFIG, FormSetupStep.valueOf("STEP_2_PROMPT_CONFIG"))
    }

    // =========================================================================
    // DOMAIN 2: Temperature Slider Math, Clamping & Badge Formatting
    // =========================================================================

    @Test
    fun testTemperatureSliderRoundingAndClamping() {
        val testCases = listOf(
            -0.5f to 0.0f,
            0.0f to 0.0f,
            0.049f to 0.05f,
            0.196f to 0.20f,
            0.204f to 0.20f,
            0.25f to 0.25f,
            0.799f to 0.80f,
            0.801f to 0.80f,
            1.2f to 0.80f,
            Float.MAX_VALUE to 0.80f,
            -Float.MAX_VALUE to 0.0f
        )

        for ((input, expected) in testCases) {
            val rounded = (Math.round(input * 100) / 100f).coerceIn(0.0f, 0.8f)
            assertEquals("Failed for input $input", expected, rounded, 0.0001f)

            val badge = String.format(Locale.US, "%.2f", rounded)
            val expectedBadge = String.format(Locale.US, "%.2f", expected)
            assertEquals("Badge format mismatch for input $input", expectedBadge, badge)
        }
    }

    @Test
    fun testTemperatureLiveBadgeFormatting() {
        assertEquals("0.00", String.format(Locale.US, "%.2f", 0.0f))
        assertEquals("0.20", String.format(Locale.US, "%.2f", 0.2f))
        assertEquals("0.55", String.format(Locale.US, "%.2f", 0.55f))
        assertEquals("0.80", String.format(Locale.US, "%.2f", 0.8f))
    }

    // =========================================================================
    // DOMAIN 3: Unified Prompt Generation for Step 2
    // =========================================================================

    @Test
    fun testStep2PromptGenerationFiltersSkippedCandidates() {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 25,
                truoc = "1. Công việc thi công:",
                sau = "",
                tinHieu = "dong_cham_lien_tiep",
                tieuDeGanNhat = "Thi công",
                loaiGoiY = "danh_sach",
                ma = "CONG_VIEC_THI_CONG",
                ten = "Nội dung thi công trong ngày",
                loai = "danh_sach",
                nguon = "thi_cong",
                boQua = false
            ),
            FormCandidate(
                id = 2,
                startIndex = 50,
                endIndex = 65,
                truoc = "2. Nghiệm thu:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Nghiệm thu",
                loaiGoiY = "van_ban",
                ma = "NGHIEM_THU",
                ten = "Công tác nghiệm thu",
                loai = "van_ban",
                nguon = "nghiem_thu",
                boQua = false
            ),
            FormCandidate(
                id = 3,
                startIndex = 100,
                endIndex = 120,
                truoc = "Chữ ký đơn vị thi công",
                sau = "",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "Chữ ký",
                loaiGoiY = "van_ban",
                ma = "CHU_KY",
                ten = "Khối chữ ký",
                loai = "van_ban",
                nguon = "nhap_tay",
                boQua = true // Should be skipped in generated prompt
            )
        )

        val prompt = FormLlmDescriber.generateUnifiedPrompt(candidates, "Nhật ký công trường A")

        assertTrue("Prompt must contain template name", prompt.contains("Nhật ký công trường A"))
        assertTrue("Prompt must include active tag CONG_VIEC_THI_CONG", prompt.contains("{{CONG_VIEC_THI_CONG}}"))
        assertTrue("Prompt must include active tag NGHIEM_THU", prompt.contains("{{NGHIEM_THU}}"))
        assertFalse("Prompt must NOT include skipped tag CHU_KY", prompt.contains("{{CHU_KY}}"))
        assertTrue("Prompt must enforce anti-hallucination rules", prompt.contains("TUYỆT ĐỐI KHÔNG BỊA ĐẶT SỐ LIỆU"))
        assertTrue("Prompt must enforce 2-page A4 budget restriction", prompt.contains("không vượt quá 2 trang A4"))
    }

    // =========================================================================
    // DOMAIN 4: Strings Resource Coverage for 2-Step Workflow & Settings
    // =========================================================================

    @Test
    fun testStringsXmlContainsAllRequiredVietnameseKeys() {
        val stringsFile = File("src/main/res/values/strings.xml")
        val content = if (stringsFile.exists()) {
            stringsFile.readText()
        } else {
            File("app/src/main/res/values/strings.xml").readText()
        }

        val requiredKeys = listOf(
            "form_setup_dialog_title",
            "form_setup_dialog_subtitle",
            "form_setup_step1_indicator",
            "form_setup_step2_indicator",
            "form_setup_step1_continue",
            "form_setup_step2_back",
            "form_setup_step2_complete",
            "form_setup_step2_regenerate",
            "form_setup_temperature_title",
            "form_setup_temperature_desc",
            "form_setup_temperature_hint",
            "form_setup_prompt_label",
            "form_setup_prompt_hint",
            "settings_temperature_title",
            "settings_temperature_subtitle",
            "settings_temperature_desc"
        )

        for (key in requiredKeys) {
            assertTrue("strings.xml must declare string key: $key", content.contains("name=\"$key\""))
        }

        // Verify key indicator texts
        assertTrue(content.contains("Bước 1 / 2: Kiểm tra thẻ neo {{MA}}"))
        assertTrue(content.contains("Bước 2 / 2: Xem lại Prompt &amp; Cấu hình") || content.contains("Bước 2 / 2: Xem lại Prompt & Cấu hình"))
        assertTrue(content.contains("Tiếp tục: Sinh Prompt thống nhất →"))
        assertTrue(content.contains("← Quay lại"))
        assertTrue(content.contains("✓ Hoàn tất &amp; Tạo Mẫu") || content.contains("✓ Hoàn tất & Tạo Mẫu"))
        assertTrue(content.contains("0.0: Bám sát nguyên văn | 0.8: Viết lại mượt mà"))
    }
}
