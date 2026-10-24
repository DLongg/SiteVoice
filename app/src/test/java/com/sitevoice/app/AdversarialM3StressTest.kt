package com.sitevoice.app

import com.sitevoice.app.data.NoteEntity
import com.sitevoice.app.llm.LlmClient
import com.sitevoice.app.llm.LlmMessage
import com.sitevoice.app.llm.ReportSynthesizer
import com.sitevoice.app.settings.SecurePreferences
import com.sitevoice.app.template.SummaryFieldConfig
import com.sitevoice.app.template.TemplateSchema
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Empirical Adversarial Stress Test Suite for Milestone M3 (Requirement R3).
 * Focus areas:
 * 1. Temperature clamping across boundaries (-1.0f, 0.0f, 0.2f, 0.8f, 1.5f, Float.MAX_VALUE, infinities).
 * 2. Dynamic JSON generation across various TemplateSchema structures (1 field, 20 fields, all manual, mixed types).
 */
class AdversarialM3StressTest {

    // =========================================================================
    // DOMAIN 1: Temperature Clamping across Boundaries and Edge Cases
    // =========================================================================

    @Test
    fun testTemperatureClampingBoundaries() {
        val fakePrefs = FakeSharedPreferences()
        val securePrefs = SecurePreferences(fakePrefs)

        // 1.1 Default value check
        assertEquals(0.2f, securePrefs.llmTemperature, 0.00001f)

        // 1.2 Explicit boundary checks requested: -1.0f, 0.0f, 0.2f, 0.8f, 1.5f, Float.MAX_VALUE
        securePrefs.llmTemperature = -1.0f
        assertEquals("Temperature -1.0f must clamp to 0.0f", 0.0f, securePrefs.llmTemperature, 0.00001f)
        assertEquals(0.0f, fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] as Float, 0.00001f)

        securePrefs.llmTemperature = 0.0f
        assertEquals("Temperature 0.0f must remain 0.0f", 0.0f, securePrefs.llmTemperature, 0.00001f)

        securePrefs.llmTemperature = 0.2f
        assertEquals("Temperature 0.2f must remain 0.2f", 0.2f, securePrefs.llmTemperature, 0.00001f)

        securePrefs.llmTemperature = 0.8f
        assertEquals("Temperature 0.8f must remain 0.8f", 0.8f, securePrefs.llmTemperature, 0.00001f)

        securePrefs.llmTemperature = 1.5f
        assertEquals("Temperature 1.5f must clamp to 0.8f", 0.8f, securePrefs.llmTemperature, 0.00001f)
        assertEquals(0.8f, fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] as Float, 0.00001f)

        securePrefs.llmTemperature = Float.MAX_VALUE
        assertEquals("Temperature Float.MAX_VALUE must clamp to 0.8f", 0.8f, securePrefs.llmTemperature, 0.00001f)

        // 1.3 Extreme Float edge cases: -Float.MAX_VALUE, infinities, near-boundary epsilons
        securePrefs.llmTemperature = -Float.MAX_VALUE
        assertEquals("Temperature -Float.MAX_VALUE must clamp to 0.0f", 0.0f, securePrefs.llmTemperature, 0.00001f)

        securePrefs.llmTemperature = Float.POSITIVE_INFINITY
        assertEquals("Temperature Float.POSITIVE_INFINITY must clamp to 0.8f", 0.8f, securePrefs.llmTemperature, 0.00001f)

        securePrefs.llmTemperature = Float.NEGATIVE_INFINITY
        assertEquals("Temperature Float.NEGATIVE_INFINITY must clamp to 0.0f", 0.0f, securePrefs.llmTemperature, 0.00001f)

        securePrefs.llmTemperature = -0.000001f
        assertEquals("Temperature -0.000001f must clamp to 0.0f", 0.0f, securePrefs.llmTemperature, 0.00001f)

        securePrefs.llmTemperature = 0.000001f
        assertEquals("Temperature 0.000001f must remain preserved", 0.000001f, securePrefs.llmTemperature, 0.00001f)

        securePrefs.llmTemperature = 0.799999f
        assertEquals("Temperature 0.799999f must remain preserved", 0.799999f, securePrefs.llmTemperature, 0.00001f)

        securePrefs.llmTemperature = 0.800001f
        assertEquals("Temperature 0.800001f must clamp to 0.8f", 0.8f, securePrefs.llmTemperature, 0.00001f)

        // 1.4 Corrupted direct storage defense: simulate raw values injected into SharedPreferences
        fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] = 999.0f
        assertEquals("Direct stored 999.0f must clamp to 0.8f on read", 0.8f, securePrefs.llmTemperature, 0.00001f)

        fakePrefs.data[SecurePreferences.KEY_LLM_TEMPERATURE] = -42.0f
        assertEquals("Direct stored -42.0f must clamp to 0.0f on read", 0.0f, securePrefs.llmTemperature, 0.00001f)

        fakePrefs.data.remove(SecurePreferences.KEY_LLM_TEMPERATURE)
        assertEquals("Missing key must fall back to DEFAULT_LLM_TEMPERATURE (0.2f)", 0.2f, securePrefs.llmTemperature, 0.00001f)
    }

    @Test
    fun testTemperaturePropagationThroughReportSynthesizer() {
        runBlocking {
            val schema = TemplateSchema(
                id = "temp_test_schema",
                name = "Test Schema",
                groupByType = "ngay",
                maxPages = 1,
                noteCategories = emptyList(),
                summaryFields = mapOf("muc_1" to SummaryFieldConfig(type = "van_ban"))
            )

            // Test 1: Default constructor temperature
            val recorderDefault = RecordingLlmClient("{}")
            val synthesizerDefault = ReportSynthesizer(recorderDefault, schema)
            synthesizerDefault.synthesizeReport("2026-10-24", emptyList())
            assertEquals(0.2f, recorderDefault.lastTemperature ?: 0f, 0.00001f)

            // Test 2: Custom constructor temperature with boundary values
            val boundaries = listOf(0.0f, 0.1f, 0.35f, 0.8f)
            for (t in boundaries) {
                val recorder = RecordingLlmClient("{}")
                val synthesizer = ReportSynthesizer(recorder, schema, temperature = t)
                synthesizer.synthesizeReport("2026-10-24", emptyList())
                assertEquals("Expected temperature $t to propagate to chat", t, recorder.lastTemperature ?: -1f, 0.00001f)
            }

            // Test 3: Method override temperature takes precedence over constructor
            val recorderOverride = RecordingLlmClient("{}")
            val synthesizerOverride = ReportSynthesizer(recorderOverride, schema, temperature = 0.1f)
            synthesizerOverride.synthesizeReport("2026-10-24", emptyList(), temperature = 0.77f)
            assertEquals("Method override temperature 0.77f must take precedence", 0.77f, recorderOverride.lastTemperature ?: -1f, 0.00001f)
        }
    }

    // =========================================================================
    // DOMAIN 2: Dynamic JSON Generation across TemplateSchema Structures
    // =========================================================================

    private fun extractTargetJson(userPrompt: String): String {
        val marker = "Cấu trúc JSON mong muốn:"
        assertTrue("Prompt must contain target JSON marker", userPrompt.contains(marker))
        val afterMarker = userPrompt.substringAfter(marker).trim()
        val firstBrace = afterMarker.indexOf('{')
        val lastBrace = afterMarker.lastIndexOf('}')
        assertTrue("Prompt must contain valid JSON delimiters", firstBrace != -1 && lastBrace > firstBrace)
        return afterMarker.substring(firstBrace, lastBrace + 1)
    }

    @Test
    fun testDynamicJsonWithSingleField() {
        runBlocking {
            // Case 2.1a: Exactly 1 field, list type
            val singleListSchema = TemplateSchema(
                id = "single_list_schema",
                name = "Biểu mẫu 1 trường danh sách",
                groupByType = "ngay",
                maxPages = 1,
                noteCategories = listOf("cong_viec"),
                summaryFields = mapOf(
                    "danh_sach_cong_tac" to SummaryFieldConfig(type = "danh_sach", maxLines = 15)
                )
            )

            val recorderList = RecordingLlmClient("{}")
            val synthesizerList = ReportSynthesizer(recorderList, singleListSchema)
            synthesizerList.synthesizeReport("2026-10-24", emptyList())

            val userPromptList = recorderList.lastMessages?.find { it.role == "user" }?.content
            assertNotNull("User prompt must be present", userPromptList)
            val jsonStrList = extractTargetJson(userPromptList!!)
            val jsonObjList = JSONObject(jsonStrList)

            assertEquals("Should have exactly 1 JSON key", 1, jsonObjList.length())
            assertTrue("Key danh_sach_cong_tac must exist", jsonObjList.has("danh_sach_cong_tac"))
            assertTrue("Value must be a JSONArray", jsonObjList.get("danh_sach_cong_tac") is JSONArray)
            assertEquals(0, jsonObjList.getJSONArray("danh_sach_cong_tac").length())

            // Case 2.1b: Exactly 1 field, text type
            val singleTextSchema = TemplateSchema(
                id = "single_text_schema",
                name = "Biểu mẫu 1 trường văn bản",
                groupByType = "ngay",
                maxPages = 1,
                noteCategories = listOf("ghi_chu"),
                summaryFields = mapOf(
                    "noi_dung_chinh" to SummaryFieldConfig(type = "van_ban", maxChars = 300)
                )
            )

            val recorderText = RecordingLlmClient("{}")
            val synthesizerText = ReportSynthesizer(recorderText, singleTextSchema)
            synthesizerText.synthesizeReport("2026-10-24", emptyList())

            val userPromptText = recorderText.lastMessages?.find { it.role == "user" }?.content
            assertNotNull("User prompt must be present", userPromptText)
            val jsonStrText = extractTargetJson(userPromptText!!)
            val jsonObjText = JSONObject(jsonStrText)

            assertEquals("Should have exactly 1 JSON key", 1, jsonObjText.length())
            assertTrue("Key noi_dung_chinh must exist", jsonObjText.has("noi_dung_chinh"))
            assertEquals("", jsonObjText.getString("noi_dung_chinh"))
        }
    }

    @Test
    fun testDynamicJsonWithTwentyFieldsStress() {
        runBlocking {
            // Case 2.2: Large schema with 20 distinct fields of mixed types
            val fieldsMap = mutableMapOf<String, SummaryFieldConfig>()
            // 10 list types
            for (i in 1..10) {
                val typeName = if (i % 2 == 0) "danh_sach" else "list"
                fieldsMap["danh_sach_muc_$i"] = SummaryFieldConfig(type = typeName, maxLines = i * 2)
            }
            // 10 mixed non-list types
            val nonListTypes = listOf("van_ban", "so", "ngay", "o_tich", "chuoi", "text", "number", "date", "checkbox", "string")
            for (i in 1..10) {
                fieldsMap["truong_van_ban_$i"] = SummaryFieldConfig(type = nonListTypes[i - 1], maxChars = i * 50)
            }

            assertEquals(20, fieldsMap.size)

            val bigSchema = TemplateSchema(
                id = "schema_20_fields",
                name = "Biểu mẫu 20 trường tổng hợp",
                groupByType = "ngay",
                maxPages = 3,
                noteCategories = listOf("tong_hop"),
                summaryFields = fieldsMap
            )

            val recorder = RecordingLlmClient("{}")
            val synthesizer = ReportSynthesizer(recorder, bigSchema)
            synthesizer.synthesizeReport("2026-10-24", emptyList())

            val userPrompt = recorder.lastMessages?.find { it.role == "user" }?.content
            assertNotNull("User prompt must be present", userPrompt)

            val jsonStr = extractTargetJson(userPrompt!!)
            // Ensure strictly valid JSON parsing without syntax error
            val jsonObj = JSONObject(jsonStr)

            assertEquals("Generated JSON must have exactly 20 keys", 20, jsonObj.length())

            // Verify each of the 10 list types is JSONArray
            for (i in 1..10) {
                val key = "danh_sach_muc_$i"
                assertTrue("Expected key $key in JSON", jsonObj.has(key))
                assertTrue("Expected $key to be JSONArray", jsonObj.get(key) is JSONArray)
            }

            // Verify each of the 10 non-list types is String ""
            for (i in 1..10) {
                val key = "truong_van_ban_$i"
                assertTrue("Expected key $key in JSON", jsonObj.has(key))
                assertEquals("Expected $key to be empty string", "", jsonObj.getString(key))
            }
        }
    }

    @Test
    fun testDynamicJsonWithAllManualFieldsFallback() {
        runBlocking {
            // Case 2.3: All fields are marked as manual or weather input
            val allManualMap = mapOf(
                "ngay" to SummaryFieldConfig(type = "ngay", sourceCategories = listOf("nhap_tay")),
                "thang" to SummaryFieldConfig(type = "so", sourceCategories = listOf("nhap_tay")),
                "nam" to SummaryFieldConfig(type = "so", sourceCategories = listOf("nhap_tay")),
                "thoi_tiet_sang" to SummaryFieldConfig(type = "o_tich", sourceCategories = listOf("thoi_tiet")),
                "thoi_tiet_chieu" to SummaryFieldConfig(type = "o_tich", sourceCategories = listOf("THOI_TIET")),
                "ky_ten" to SummaryFieldConfig(type = "van_ban", sourceCategories = listOf("NHAP_TAY"))
            )

            val manualOnlySchema = TemplateSchema(
                id = "manual_only_schema",
                name = "Biểu mẫu toàn bộ trường nhập tay",
                groupByType = "ngay",
                maxPages = 1,
                noteCategories = emptyList(),
                summaryFields = allManualMap
            )

            val recorder = RecordingLlmClient("{}")
            val synthesizer = ReportSynthesizer(recorder, manualOnlySchema)
            synthesizer.synthesizeReport("2026-10-24", emptyList())

            val userPrompt = recorder.lastMessages?.find { it.role == "user" }?.content
            assertNotNull("User prompt must be present", userPrompt)

            val jsonStr = extractTargetJson(userPrompt!!)
            val jsonObj = JSONObject(jsonStr)

            // When all fields are manual, eligible is empty -> must fall back to standard 7 fields
            val standard7Keys = listOf(
                "cong_viec_thi_cong",
                "nghiem_thu",
                "an_toan",
                "y_kien_cdt",
                "y_kien_tvgs",
                "y_kien_nha_thau",
                "chua_phan_loai"
            )

            assertEquals(7, jsonObj.length())
            for (k in standard7Keys) {
                assertTrue("Fallback JSON must contain standard key $k", jsonObj.has(k))
            }

            // Verify none of the manual fields leaked into the fallback JSON
            for (manualKey in allManualMap.keys) {
                assertFalse("Manual field $manualKey must not exist in fallback JSON", jsonObj.has(manualKey))
            }
        }
    }

    @Test
    fun testDynamicJsonWithMixedManualAndDynamicFields() {
        runBlocking {
            // Case 2.4: 10 manual fields + 10 dynamic synthesis fields
            val mixedFields = mutableMapOf<String, SummaryFieldConfig>()

            // 10 manual fields
            for (i in 1..5) {
                mixedFields["manual_field_$i"] = SummaryFieldConfig(type = "so", sourceCategories = listOf("nhap_tay"))
            }
            for (i in 6..10) {
                mixedFields["weather_field_$i"] = SummaryFieldConfig(type = "o_tich", sourceCategories = listOf("thoi_tiet"))
            }

            // 10 dynamic fields (5 list, 5 text)
            for (i in 1..5) {
                mixedFields["dynamic_list_$i"] = SummaryFieldConfig(type = "danh_sach", sourceCategories = listOf("thi_cong"))
            }
            for (i in 6..10) {
                mixedFields["dynamic_text_$i"] = SummaryFieldConfig(type = "van_ban", sourceCategories = listOf("danh_gia"))
            }

            assertEquals(20, mixedFields.size)

            val mixedSchema = TemplateSchema(
                id = "mixed_schema",
                name = "Biểu mẫu nửa nhập tay nửa AI",
                groupByType = "ngay",
                maxPages = 2,
                noteCategories = listOf("thi_cong", "danh_gia"),
                summaryFields = mixedFields
            )

            val recorder = RecordingLlmClient("{}")
            val synthesizer = ReportSynthesizer(recorder, mixedSchema)
            synthesizer.synthesizeReport("2026-10-24", emptyList())

            val userPrompt = recorder.lastMessages?.find { it.role == "user" }?.content
            assertNotNull(userPrompt)

            val jsonStr = extractTargetJson(userPrompt!!)
            val jsonObj = JSONObject(jsonStr)

            // Must have exactly 10 fields (the dynamic ones)
            assertEquals("Target JSON must contain exactly 10 dynamic fields", 10, jsonObj.length())

            // Verify 10 dynamic fields are present
            for (i in 1..5) {
                assertTrue("Dynamic list $i must exist", jsonObj.has("dynamic_list_$i"))
                assertTrue("Dynamic list $i must be array", jsonObj.get("dynamic_list_$i") is JSONArray)
            }
            for (i in 6..10) {
                assertTrue("Dynamic text $i must exist", jsonObj.has("dynamic_text_$i"))
                assertEquals("", jsonObj.getString("dynamic_text_$i"))
            }

            // Verify 10 manual fields are strictly excluded
            for (i in 1..5) {
                assertFalse("Manual field $i must be excluded", jsonObj.has("manual_field_$i"))
            }
            for (i in 6..10) {
                assertFalse("Weather field $i must be excluded", jsonObj.has("weather_field_$i"))
            }
        }
    }

    @Test
    fun testDynamicJsonWithEmptySummaryFields() {
        runBlocking {
            // Case 2.5: summaryFields is totally empty
            val emptySchema = TemplateSchema(
                id = "empty_schema",
                name = "Biểu mẫu trống",
                groupByType = "ngay",
                maxPages = 1,
                noteCategories = emptyList(),
                summaryFields = emptyMap()
            )

            val recorder = RecordingLlmClient("{}")
            val synthesizer = ReportSynthesizer(recorder, emptySchema)
            synthesizer.synthesizeReport("2026-10-24", emptyList())

            val userPrompt = recorder.lastMessages?.find { it.role == "user" }?.content
            assertNotNull(userPrompt)

            val jsonStr = extractTargetJson(userPrompt!!)
            val jsonObj = JSONObject(jsonStr)

            assertEquals("Fallback to standard 7 fields", 7, jsonObj.length())
            assertTrue(jsonObj.has("cong_viec_thi_cong"))
            assertTrue(jsonObj.has("nghiem_thu"))
            assertTrue(jsonObj.has("an_toan"))
            assertTrue(jsonObj.has("y_kien_cdt"))
            assertTrue(jsonObj.has("y_kien_tvgs"))
            assertTrue(jsonObj.has("y_kien_nha_thau"))
            assertTrue(jsonObj.has("chua_phan_loai"))
        }
    }

    @Test
    fun testBuiltinConstructionTemplateStructure() {
        runBlocking {
            // Case 2.6: Builtin construction journal template schema
            val schemaFile = File("src/main/assets/templates/nhat-ky-cong-truong/schema.json")
            val altFile = File("app/src/main/assets/templates/nhat-ky-cong-truong/schema.json")
            val target = if (schemaFile.exists()) schemaFile else altFile
            val builtinSchema = TemplateSchema.fromJson(target.readText(Charsets.UTF_8))

            val recorder = RecordingLlmClient("{}")
            val synthesizer = ReportSynthesizer(recorder, builtinSchema)
            synthesizer.synthesizeReport("2026-10-24", emptyList())

            val userPrompt = recorder.lastMessages?.find { it.role == "user" }?.content
            assertNotNull(userPrompt)

            val jsonStr = extractTargetJson(userPrompt!!)
            val jsonObj = JSONObject(jsonStr)

            assertTrue(jsonObj.has("cong_viec_thi_cong"))
            val thiCongArr = jsonObj.getJSONArray("cong_viec_thi_cong")
            assertTrue("Expected structured sample entries in cong_viec_thi_cong", thiCongArr.length() >= 4)
            assertEquals("3.1 Điện", thiCongArr.getString(0))
            assertEquals("\t- [Nội dung và vị trí]", thiCongArr.getString(1))
            assertEquals("3.2 Nước", thiCongArr.getString(2))
            assertEquals("\t- [Nội dung và vị trí]", thiCongArr.getString(3))
        }
    }

    @Test
    fun testDynamicJsonWithCustomTemplateContainingCongViecThiCong() {
        runBlocking {
            // Case 2.7: A custom template (id != "nhat-ky-cong-truong") that also defines "cong_viec_thi_cong"
            val customSchema = TemplateSchema(
                id = "custom_bridge_project",
                name = "Nhật ký thi công cầu",
                groupByType = "ngay",
                maxPages = 2,
                noteCategories = listOf("thi_cong", "kiem_dinh"),
                summaryFields = mapOf(
                    "cong_viec_thi_cong" to SummaryFieldConfig(type = "danh_sach", maxLines = 20),
                    "kiem_dinh_vat_lieu" to SummaryFieldConfig(type = "danh_sach", maxLines = 10)
                )
            )

            val recorder = RecordingLlmClient("{}")
            val synthesizer = ReportSynthesizer(recorder, customSchema)
            synthesizer.synthesizeReport("2026-10-24", emptyList())

            val userPrompt = recorder.lastMessages?.find { it.role == "user" }?.content
            assertNotNull(userPrompt)

            val jsonStr = extractTargetJson(userPrompt!!)
            val jsonObj = JSONObject(jsonStr)

            assertEquals(2, jsonObj.length())
            assertTrue(jsonObj.has("cong_viec_thi_cong"))
            assertTrue(jsonObj.has("kiem_dinh_vat_lieu"))

            val thiCongArr = jsonObj.getJSONArray("cong_viec_thi_cong")
            assertEquals("3.1 Điện", thiCongArr.getString(0))

            val kiemDinhArr = jsonObj.getJSONArray("kiem_dinh_vat_lieu")
            assertEquals(0, kiemDinhArr.length())
        }
    }

    // =========================================================================
    // DOMAIN 3: End-to-End Synthesize Execution with Clamped Temperature & Notes
    // =========================================================================

    @Test
    fun testEndToEndSynthesizeExecutionWithClampedTemperatureAndNotes() {
        runBlocking {
            val fakePrefs = FakeSharedPreferences()
            val securePrefs = SecurePreferences(fakePrefs)

            // User sets temperature to extreme boundary 1.2f -> clamped to 0.8f
            securePrefs.llmTemperature = 1.2f
            assertEquals(0.8f, securePrefs.llmTemperature, 0.00001f)

            val schema = TemplateSchema(
                id = "e2e_stress_schema",
                name = "E2E Stress Schema",
                groupByType = "ngay",
                maxPages = 1,
                noteCategories = listOf("thi_cong", "an_toan"),
                summaryFields = mapOf(
                    "cong_tac" to SummaryFieldConfig(type = "danh_sach", maxLines = 5),
                    "ghi_chu_an_toan" to SummaryFieldConfig(type = "van_ban", maxChars = 100)
                )
            )

            val mockLlmResponse = """
            {
              "cong_tac": ["Ép cọc D400 hoàn thành 12 tim"],
              "ghi_chu_an_toan": "Đảm bảo an toàn không có sự cố"
            }
            """.trimIndent()

            val recordingClient = RecordingLlmClient(mockLlmResponse)
            val synthesizer = ReportSynthesizer(
                llmClient = recordingClient,
                schema = schema,
                temperature = securePrefs.llmTemperature
            )

            val notes = listOf(
                NoteEntity(
                    createdAt = 1000L,
                    date = "2026-10-24",
                    audioPath = "/audio/1.m4a",
                    durationMs = 3000,
                    text = "Ép cọc D400 xong 12 tim",
                    label = "thi_cong",
                    status = "LABELED"
                ),
                NoteEntity(
                    createdAt = 2000L,
                    date = "2026-10-24",
                    audioPath = "/audio/2.m4a",
                    durationMs = 2500,
                    text = "Hôm nay an toàn tốt",
                    label = "an_toan",
                    status = "LABELED"
                )
            )

            val result = synthesizer.synthesizeReport("2026-10-24", notes)
            assertTrue("Synthesize report should succeed", result.isSuccess)

            // Verify temperature passed to LLM client was exactly the clamped 0.8f
            assertEquals(0.8f, recordingClient.lastTemperature ?: 0f, 0.00001f)
            assertEquals(true, recordingClient.lastResponseJson)

            // Verify parsed output
            val outputJson = JSONObject(result.getOrThrow())
            assertTrue(outputJson.has("cong_tac"))
            assertEquals(1, outputJson.getJSONArray("cong_tac").length())
            assertEquals("Đảm bảo an toàn không có sự cố", outputJson.getString("ghi_chu_an_toan"))
        }
    }
}
