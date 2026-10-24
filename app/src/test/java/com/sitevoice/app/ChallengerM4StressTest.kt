package com.sitevoice.app

import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.export.DailyLogEntity
import com.sitevoice.app.export.GoogleDocsExporter
import com.sitevoice.app.export.PlaceholderMapper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.regex.Pattern

/**
 * Empirical stress tests for Milestone M4 (Requirement R4)
 * Executed by challenger_m4_2.
 */
class ChallengerM4StressTest {

    private val pattern: Pattern = GoogleDocsExporter.LINGERING_TAG_PATTERN

    private fun findTags(text: String): List<String> {
        val matches = mutableListOf<String>()
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            matches.add(matcher.group())
        }
        return matches
    }

    // =========================================================================
    // PART 1: Regex Pattern Stress Testing: \{\{[^{}\r\n]{1,100}\}\}
    // =========================================================================

    @Test
    fun testRegexNumbers() {
        val sample = "Numbers in tags: {{123}} and {{TAG_2026}} and {{1.1}} and {{3-1}} and {{42}}"
        val matches = findTags(sample)
        assertEquals(listOf("{{123}}", "{{TAG_2026}}", "{{1.1}}", "{{3-1}}", "{{42}}"), matches)
    }

    @Test
    fun testRegexUnderscores() {
        val sample = "Underscores: {{MY_TAG_FIELD}} and {{___}} and {{_}} and {{PREFIX___SUFFIX}}"
        val matches = findTags(sample)
        assertEquals(listOf("{{MY_TAG_FIELD}}", "{{___}}", "{{_}}", "{{PREFIX___SUFFIX}}"), matches)
    }

    @Test
    fun testRegexColons() {
        val sample = "Colons: {{FIELD:NAME}} and {{ns:prop:val}} and {{tag:}} and {{:prefix}}"
        val matches = findTags(sample)
        assertEquals(listOf("{{FIELD:NAME}}", "{{ns:prop:val}}", "{{tag:}}", "{{:prefix}}"), matches)
    }

    @Test
    fun testRegexSpaces() {
        val sample = "Spaces: {{ TAG }} and {{   SPACED_TAG   }} and {{TAG WITH SPACES}} and {{ }}"
        val matches = findTags(sample)
        // {{ }} has 1 space between braces -> length 1 -> matches!
        assertEquals(listOf("{{ TAG }}", "{{   SPACED_TAG   }}", "{{TAG WITH SPACES}}", "{{ }}"), matches)
    }

    @Test
    fun testRegexUnicode() {
        val sample = "Unicode: {{TIÊU_ĐỀ}} and {{KỸ_THUẬT}} and {{ĐẶNG_VĂN_A}} and {{日_本_語}} and {{café}} and {{🎉}}"
        val matches = findTags(sample)
        assertEquals(
            listOf("{{TIÊU_ĐỀ}}", "{{KỸ_THUẬT}}", "{{ĐẶNG_VĂN_A}}", "{{日_本_語}}", "{{café}}", "{{🎉}}"),
            matches
        )
    }

    @Test
    fun testRegexEmptyTags() {
        val sample = "Empty tag: {{}} and single space {{ }} and two spaces {{  }}"
        val matches = findTags(sample)
        // Quantifier {1,100} requires at least 1 character!
        // Therefore, {{}} has 0 characters and does NOT match.
        // Spaces {{ }} (1 char) and {{  }} (2 chars) DO match.
        assertFalse(matches.contains("{{}}"))
        assertTrue(matches.contains("{{ }}"))
        assertTrue(matches.contains("{{  }}"))
        assertEquals(listOf("{{ }}", "{{  }}"), matches)
    }

    @Test
    fun testRegexMultiParagraphAndNewlines() {
        // 1. Tag spanning newline: must NOT match across lines
        val sampleWithNewlines = "Start {{\nTAG_SPANNING_NEWLINE\n}} End"
        val matches1 = findTags(sampleWithNewlines)
        assertTrue(matches1.isEmpty())

        val sampleWithCrLf = "Start {{\r\nTAG_SPANNING_CRLF\r\n}} End"
        val matches2 = findTags(sampleWithCrLf)
        assertTrue(matches2.isEmpty())

        // 2. Multiple tags across multi-paragraph string separated by newlines
        val multiParagraphText = """
            Paragraph 1 has {{TAG_P1}}.
            
            Paragraph 2 has {{TAG_P2}} and {{TAG_P2_EXTRA}}.
            
            Paragraph 3 has broken {{BROKEN_P3
            still broken}} but valid {{VALID_P3}}.
        """.trimIndent()
        val matchesMulti = findTags(multiParagraphText)
        assertEquals(listOf("{{TAG_P1}}", "{{TAG_P2}}", "{{TAG_P2_EXTRA}}", "{{VALID_P3}}"), matchesMulti)
        assertFalse(matchesMulti.any { it.contains("BROKEN_P3") })
    }

    @Test
    fun testRegexBoundaryLength() {
        // Tag with 1 char: {{a}} -> match
        val tag1Char = "{{a}}"
        assertEquals(listOf("{{a}}"), findTags(tag1Char))

        // Tag with exactly 100 chars inside: {{"a" * 100}} -> match
        val inner100 = "a".repeat(100)
        val tag100Chars = "{{$inner100}}"
        assertEquals(listOf(tag100Chars), findTags(tag100Chars))

        // Tag with 101 chars inside: {{"a" * 101}} -> must NOT match
        val inner101 = "a".repeat(101)
        val tag101Chars = "{{$inner101}}"
        assertTrue(findTags(tag101Chars).isEmpty())
    }

    @Test
    fun testRegexNestedAndStrayBraces() {
        // Single inner brace: {{TAG{inner}}} -> neither matches
        val singleNested = "{{TAG{inner}}}"
        assertTrue(findTags(singleNested).isEmpty())

        // Double inner brace: {{TAG{{inner}}}} -> inner matches {{inner}}
        val doubleNested = "{{TAG{{inner}}}}"
        assertEquals(listOf("{{inner}}"), findTags(doubleNested))

        // Triple braces: {{{TAG}}} -> {{TAG}} matches inside
        val tripleBraces = "{{{TAG}}}"
        val matchesTriple = findTags(tripleBraces)
        assertEquals(listOf("{{TAG}}"), matchesTriple)

        // Tag followed by closing brace: {{TAG}}}
        val trailingBrace = "{{TAG}}}"
        val matchesTrailing = findTags(trailingBrace)
        assertEquals(listOf("{{TAG}}"), matchesTrailing)
    }

    // =========================================================================
    // PART 2: PlaceholderMapper Schema Defaulting with Empty DailyLogEntity
    // =========================================================================

    @Test
    fun testPlaceholderMapperEmptyDailyLogVietnameseSchemaObject() {
        val schemaJson = """
        {
            "ten": "Báo cáo thử nghiệm VN Object",
            "truong_tong_hop": {
                "cong_viec_thi_cong": { "kieu": "danh_sach", "mo_ta": "Công việc chính" },
                "nghiem_thu": { "kieu": "danh_sach" },
                "an_toan": { "kieu": "danh_sach" },
                "tien_do_du_an": { "kieu": "van_ban", "placeholder": "{{TIEN_DO_DU_AN}}" },
                "kien_nghi_dac_biet": { "kieu": "van_ban", "ma": "KIEN_NGHI_DAC_BIET" }
            },
            "nhap_tay": {
                "chi_huy_truong": { "kieu": "chu", "placeholder": "{{CHI_HUY_TRUONG}}" },
                "giam_sat_vien": { "kieu": "chu", "name": "giam_sat_vien" },
                "ghi_chu_them": { "kieu": "van_ban" }
            }
        }
        """.trimIndent()

        val template = TemplateItemEntity(
            id = "test-vn-object",
            name = "Test VN Object",
            description = "Test",
            systemPrompt = "Test",
            schemaJson = schemaJson
        )

        // Empty DailyLogEntity with default parameters
        val emptyLog = DailyLogEntity()
        val map = PlaceholderMapper.mapToPlaceholders(emptyLog, template)

        // Declared fields to verify:
        val expectedKeys = listOf(
            "cong_viec_thi_cong", "CONG_VIEC_THI_CONG",
            "nghiem_thu", "NGHIEM_THU",
            "an_toan", "AN_TOAN",
            "tien_do_du_an", "TIEN_DO_DU_AN",
            "kien_nghi_dac_biet", "KIEN_NGHI_DAC_BIET",
            "chi_huy_truong", "CHI_HUY_TRUONG",
            "giam_sat_vien", "GIAM_SAT_VIEN",
            "ghi_chu_them", "GHI_CHU_THEM"
        )

        for (k in expectedKeys) {
            val tag = "{{$k}}"
            assertTrue("Map must contain tag $tag", map.containsKey(tag))
            assertEquals("Tag $tag must be assigned empty string \"\"", "", map[tag])

            // Also verify spaced variation
            val spacedTag = "{{ $k }}"
            assertTrue("Map must contain spaced tag $spacedTag", map.containsKey(spacedTag))
            assertEquals("Spaced tag $spacedTag must be assigned empty string \"\"", "", map[spacedTag])
        }
    }

    @Test
    fun testPlaceholderMapperEmptyDailyLogVietnameseSchemaArray() {
        val schemaJson = """
        {
            "ten": "Báo cáo thử nghiệm VN Array",
            "truong_tong_hop": [
                { "ma": "KHOI_LUONG", "kieu": "van_ban", "placeholder": "{{KHOI_LUONG}}" },
                { "ma": "VAT_TU", "kieu": "danh_sach" },
                "chat_luong_thi_cong"
            ],
            "nhap_tay": [
                { "name": "ky_su_giam_sat", "kieu": "chu" },
                "nguoi_duyet_don"
            ]
        }
        """.trimIndent()

        val template = TemplateItemEntity(
            id = "test-vn-array",
            name = "Test VN Array",
            description = "Test",
            systemPrompt = "Test",
            schemaJson = schemaJson
        )

        val emptyLog = DailyLogEntity()
        val map = PlaceholderMapper.mapToPlaceholders(emptyLog, template)

        val expectedTags = listOf(
            "{{KHOI_LUONG}}", "{{khoi_luong}}", "{{ KHOI_LUONG }}",
            "{{VAT_TU}}", "{{vat_tu}}", "{{ VAT_TU }}",
            "{{chat_luong_thi_cong}}", "{{CHAT_LUONG_THI_CONG}}",
            "{{ky_su_giam_sat}}", "{{KY_SU_GIAM_SAT}}",
            "{{nguoi_duyet_don}}", "{{NGUOI_DUYET_DON}}"
        )

        for (tag in expectedTags) {
            assertTrue("Map must contain tag $tag", map.containsKey(tag))
            assertEquals("Tag $tag must have empty string value", "", map[tag])
        }
    }

    @Test
    fun testPlaceholderMapperEmptyDailyLogEnglishSchemaArrayAndObject() {
        val schemaJson = """
        {
            "name": "Engineering Site Log EN",
            "summary_fields": [
                { "name": "structural_inspection", "type": "string", "placeholder": "{{STRUCTURAL_INSPECTION}}" },
                { "name": "mep_status", "type": "list" },
                { "key": "safety_incident_log", "type": "list" }
            ],
            "manual_fields": [
                { "name": "lead_inspector", "type": "string" },
                "site_weather_observation"
            ],
            "cho_dien": [
                "{{CUSTOM_FREE_SLOT_1}}",
                "custom_free_slot_2"
            ]
        }
        """.trimIndent()

        val template = TemplateItemEntity(
            id = "test-en-schema",
            name = "Test EN Schema",
            description = "Test",
            systemPrompt = "Test",
            schemaJson = schemaJson
        )

        val emptyLog = DailyLogEntity()
        val map = PlaceholderMapper.mapToPlaceholders(emptyLog, template)

        val expectedTags = listOf(
            "{{structural_inspection}}", "{{STRUCTURAL_INSPECTION}}", "{{ STRUCTURAL_INSPECTION }}",
            "{{mep_status}}", "{{MEP_STATUS}}",
            "{{safety_incident_log}}", "{{SAFETY_INCIDENT_LOG}}",
            "{{lead_inspector}}", "{{LEAD_INSPECTOR}}",
            "{{site_weather_observation}}", "{{SITE_WEATHER_OBSERVATION}}",
            "{{CUSTOM_FREE_SLOT_1}}", "{{custom_free_slot_1}}", "{{ CUSTOM_FREE_SLOT_1 }}",
            "{{custom_free_slot_2}}", "{{CUSTOM_FREE_SLOT_2}}"
        )

        for (tag in expectedTags) {
            assertTrue("Map must contain tag $tag", map.containsKey(tag))
            assertEquals("Tag $tag must have value \"\"", "", map[tag])
        }
    }

    @Test
    fun testPlaceholderMapperVerify100PercentDeclaredFieldsAssignedEmptyString() {
        // Generic schema with diverse field naming conventions
        val schemaJson = """
        {
            "ten": "Mẫu tổng thể 100% Phủ",
            "summaryFields": {
                "field_alpha": { "placeholder": "{{ALPHA}}" },
                "field_beta": { "ma": "BETA" },
                "field_gamma": { "key": "GAMMA" },
                "field_delta": { "name": "DELTA" },
                "field_epsilon": "mo_ta_truc_tiep"
            },
            "manualFields": {
                "manual_m1": { "placeholder": "{{M1}}" },
                "manual_m2": "m2_string"
            },
            "cho_dien": {
                "slot_s1": { "placeholder": "{{S1}}" },
                "slot_s2": "s2_text"
            }
        }
        """.trimIndent()

        val template = TemplateItemEntity(
            id = "test-100-percent",
            name = "Test 100% Defaulting",
            description = "Test",
            systemPrompt = "Test",
            schemaJson = schemaJson
        )

        val emptyLog = DailyLogEntity()
        val map = PlaceholderMapper.mapToPlaceholders(emptyLog, template)

        // Parse schema declared keys dynamically
        val declaredFieldTags = mutableSetOf<String>()
        val obj = JSONObject(schemaJson)

        fun extractDeclaredTags(container: Any?) {
            when (container) {
                is JSONObject -> {
                    val keys = container.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        declaredFieldTags.add(key)
                        val child = container.opt(key)
                        if (child is JSONObject) {
                            child.optString("placeholder").takeIf { it.isNotBlank() }?.let { declaredFieldTags.add(it) }
                            child.optString("key").takeIf { it.isNotBlank() }?.let { declaredFieldTags.add(it) }
                            child.optString("name").takeIf { it.isNotBlank() }?.let { declaredFieldTags.add(it) }
                            child.optString("ma").takeIf { it.isNotBlank() }?.let { declaredFieldTags.add(it) }
                        } else if (child is String && child.isNotBlank()) {
                            declaredFieldTags.add(child)
                        }
                    }
                }
                is JSONArray -> {
                    for (i in 0 until container.length()) {
                        val item = container.opt(i)
                        when (item) {
                            is JSONObject -> {
                                item.optString("placeholder").takeIf { it.isNotBlank() }?.let { declaredFieldTags.add(it) }
                                item.optString("key").takeIf { it.isNotBlank() }?.let { declaredFieldTags.add(it) }
                                item.optString("name").takeIf { it.isNotBlank() }?.let { declaredFieldTags.add(it) }
                                item.optString("ma").takeIf { it.isNotBlank() }?.let { declaredFieldTags.add(it) }
                            }
                            is String -> declaredFieldTags.add(item)
                        }
                    }
                }
            }
        }

        extractDeclaredTags(obj.opt("summaryFields"))
        extractDeclaredTags(obj.opt("manualFields"))
        extractDeclaredTags(obj.opt("cho_dien"))

        // Verify that 100% of declared tags have non-null empty string in map
        var checkedCount = 0
        for (rawTag in declaredFieldTags) {
            val inner = if (rawTag.startsWith("{{") && rawTag.endsWith("}}")) {
                rawTag.substring(2, rawTag.length - 2).trim()
            } else {
                rawTag.trim()
            }
            if (inner.isBlank()) continue

            val baseTag = "{{$inner}}"
            val upperTag = "{{${inner.uppercase()}}}"
            val lowerTag = "{{${inner.lowercase()}}}"

            assertTrue("Base tag $baseTag must exist in map", map.containsKey(baseTag))
            assertEquals("Base tag $baseTag must equal \"\"", "", map[baseTag])

            assertTrue("Upper tag $upperTag must exist in map", map.containsKey(upperTag))
            assertEquals("Upper tag $upperTag must equal \"\"", "", map[upperTag])

            assertTrue("Lower tag $lowerTag must exist in map", map.containsKey(lowerTag))
            assertEquals("Lower tag $lowerTag must equal \"\"", "", map[lowerTag])

            checkedCount++
        }

        assertTrue("Must have verified at least 8 declared field tags", checkedCount >= 8)
    }
}
