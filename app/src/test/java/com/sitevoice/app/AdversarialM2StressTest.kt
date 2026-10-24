package com.sitevoice.app

import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormCandidateScanner
import com.sitevoice.app.formsetup.FormLlmDescriber
import com.sitevoice.app.formsetup.FormSetupService
import com.sitevoice.app.template.TemplateSchema
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Empirical Adversarial Stress Test Suite for Milestone M2 (Requirement R2).
 * FormCandidateScanner, FormLlmDescriber, TemplateSchema, FormSetupService.
 */
class AdversarialM2StressTest {

    // =============================================================
    // DOMAIN 1: Heading regex matching across 20+ exotic patterns
    // =============================================================
    @Test
    fun testExoticHeadingRegexMatching() {
        val positiveHeadings = listOf(
            // 1. Standard Roman numerals uppercase
            "I. THÔNG TIN CHUNG",
            "II. TÌNH HÌNH THI CÔNG:",
            "III) NỘI DUNG CÔNG VIỆC",
            "IV. CÔNG TÁC NGHIỆM THU",
            "V. AN TOÀN LAO ĐỘNG",
            "VI. ĐÁNH GIÁ CHUNG",
            "VII. BIỆN PHÁP KHẮC PHỤC",
            "VIII. KẾ HOẠCH HÔM SAU",
            "IX. Ý KIẾN CÁC BÊN",
            "X. PHỤ LỤC HỒ SƠ",
            "XIV. KẾT LUẬN CUỐI CÙNG",
            "XX. BIÊN BẢN BÀN GIAO",

            // 2. Lowercase Roman numerals
            "i. thông tin ban đầu",
            "ii. nhân lực và ca kíp:",
            "iii) kiểm tra hiện trường",
            "iv. tổng kết khối lượng",
            "vi. đánh giá chất lượng",
            "viii. thiết bị chuyên dụng",
            "ix. vật liệu đầu vào",
            "xii. nghiệm thu hoàn thành",

            // 3. Letters uppercase and lowercase
            "A. THÔNG TIN DỰ ÁN",
            "B. NHÂN SỰ VÀ THIẾT BỊ:",
            "C) VẬT LIỆU THI CÔNG",
            "D. TIẾN ĐỘ THỰC HIỆN",
            "a. công tác chuẩn bị",
            "b. triển khai ép cọc:",
            "c) đổ bê tông đài móng",
            "z. lưu ý đặc biệt",

            // 4. Hierarchical sub-sections
            "1. Tình hình thời tiết:",
            "1.1. Buổi sáng nắng ráo:",
            "1.2 Buổi chiều có mưa rào nhẹ",
            "2) Lực lượng thi công trên công trường",
            "3.1.1 Nhân lực đội cốt thép:",
            "3.1.2.4 Nghiệm thu cốt thép dầm sàn tầng 5:",
            "10.2.1 Nhật ký quan trắc độ lún:",

            // 5. Descriptive headings with colons and Vietnamese diacritics (up to 150 chars)
            "Chủ đầu tư:",
            "Đơn vị tư vấn giám sát trưởng:",
            "Ý kiến chỉ đạo của Chủ đầu tư / Tư vấn quản lý dự án:",
            "Tình hình an toàn lao động và vệ sinh môi trường trên toàn bộ công trường:",
            "ĐÁNH GIÁ TÌNH HÌNH THỰC HIỆN TIẾN ĐỘ VÀ CHẤT LƯỢNG CÔNG TRÌNH THEO HỢP ĐỒNG:",
            "Ghi chú bổ sung của Ban chỉ huy công trường:"
        )

        for (h in positiveHeadings) {
            assertTrue("Expected heading pattern to match: '$h'", FormCandidateScanner.isHeading(h))
        }

        // Negative non-headings (must NOT match)
        val negativeHeadings = listOf(
            "",
            "   ",
            "\t\n",
            "................................",
            "--------------------------------",
            "________________________________",
            "Hà Nội, ngày ..... tháng ..... năm 2026",
            "- Ban chỉ huy công trường: ..... người",
            "- Sáng: trời nắng ☐ trời mưa ☑",
            "Hôm nay thời tiết đẹp trời không mưa",
            "Lưu ý: công nhân phải đội mũ bảo hộ khi làm việc tại công trường",
            "Nội dung công việc thi công hố móng sâu 3m diễn ra bình thường",
            ":",
            " : "
        )

        for (nh in negativeHeadings) {
            assertFalse("Expected non-heading to NOT match: '$nh'", FormCandidateScanner.isHeading(nh))
        }
    }

    // =============================================================
    // DOMAIN 2: Candidate deduplication with duplicate codes
    // =============================================================
    @Test
    fun testCandidateDeduplicationStress() {
        // Test 2.1: Many identical codes
        val candidatesSameCode = (1..10).map { id ->
            FormCandidate(
                id = id,
                startIndex = id * 10,
                endIndex = id * 10 + 5,
                truoc = "Trường $id",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Mục thi công",
                loaiGoiY = "van_ban",
                ma = "CONG_VIEC"
            )
        }

        val dedup1 = FormLlmDescriber.deduplicateCandidateCodes(candidatesSameCode)
        val codes1 = dedup1.map { it.ma }
        assertEquals(10, codes1.distinct().size)
        assertEquals("CONG_VIEC", codes1[0])
        assertEquals("CONG_VIEC_2", codes1[1])
        assertEquals("CONG_VIEC_3", codes1[2])
        assertEquals("CONG_VIEC_10", codes1[9])

        // Test 2.2: Pre-existing suffixed keys and collision resolution
        val candidatesMixed = listOf(
            FormCandidate(id = 1, startIndex = 0, endIndex = 5, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "VAT_TU"),
            FormCandidate(id = 2, startIndex = 10, endIndex = 15, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "VAT_TU_2"),
            FormCandidate(id = 3, startIndex = 20, endIndex = 25, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "VAT_TU"),
            FormCandidate(id = 4, startIndex = 30, endIndex = 35, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "VAT_TU")
        )

        val dedup2 = FormLlmDescriber.deduplicateCandidateCodes(candidatesMixed)
        val codes2 = dedup2.map { it.ma }
        assertEquals(4, codes2.distinct().size)
        assertEquals("VAT_TU", codes2[0])
        assertEquals("VAT_TU_2", codes2[1])
        assertEquals("VAT_TU_3", codes2[2]) // should skip _2 and pick _3
        assertEquals("VAT_TU_4", codes2[3])

        // Test 2.3: Dirty code sanitization (spaces, lowercase, symbols, Vietnamese chars)
        val candidatesDirty = listOf(
            FormCandidate(id = 1, startIndex = 0, endIndex = 5, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "  cong  viec  thi cong !@# "),
            FormCandidate(id = 2, startIndex = 10, endIndex = 15, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "  "),
            FormCandidate(id = 3, startIndex = 20, endIndex = 25, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "???"),
            FormCandidate(id = 4, startIndex = 30, endIndex = 35, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "CHU_KY", boQua = true),
            FormCandidate(id = 5, startIndex = 40, endIndex = 45, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "CHU_KY", boQua = true)
        )

        val dedupDirty = FormLlmDescriber.deduplicateCandidateCodes(candidatesDirty)
        assertTrue(dedupDirty[0].ma.matches(Regex("^[A-Z0-9_]+$")))
        assertEquals("TRUONG_2", dedupDirty[1].ma)
        assertEquals("TRUONG_3", dedupDirty[2].ma)
        // boQua candidates must remain unchanged
        assertEquals("CHU_KY", dedupDirty[3].ma)
        assertEquals("CHU_KY", dedupDirty[4].ma)
    }

    // =============================================================
    // DOMAIN 3: Bilingual JSON parsing in TemplateSchema
    // =============================================================
    @Test
    fun testBilingualAndHybridSchemaParsing() {
        // 3.1: Pure Vietnamese Schema
        val vnJson = """
            {
                "id": "mau_vn_pure",
                "ten": "Mẫu Nhật Ký Việt Nam",
                "gioi_han_trang": 2,
                "gom_theo": "ngay",
                "muc_ghi_chu": ["thi_cong", "nghiem_thu"],
                "truong_tong_hop": {
                    "cong_viec": {
                        "kieu": "danh_sach",
                        "toi_da_dong": 10,
                        "ky_tu_moi_dong": 85,
                        "muc_nguon": ["thi_cong"],
                        "hien_trong_doc": true
                    },
                    "y_kien": {
                        "kieu": "van_ban",
                        "toi_da_ky_tu": 250,
                        "nguon": "y_kien_tvgs"
                    }
                }
            }
        """.trimIndent()
        val vnSchema = TemplateSchema.fromJson(vnJson)
        assertEquals("mau_vn_pure", vnSchema.id)
        assertEquals("Mẫu Nhật Ký Việt Nam", vnSchema.name)
        assertEquals(2, vnSchema.maxPages)
        assertEquals(2, vnSchema.summaryFields.size)
        assertEquals(10, vnSchema.summaryFields["cong_viec"]?.maxLines)
        assertEquals(85, vnSchema.summaryFields["cong_viec"]?.maxCharsPerLine)
        assertEquals(250, vnSchema.summaryFields["y_kien"]?.maxChars)

        // 3.2: Pure English Schema
        val enJson = """
            {
                "id": "template_en_pure",
                "name": "English Daily Log",
                "max_pages": 4,
                "group_by": "tuan",
                "note_categories": ["general", "safety"],
                "summary_fields": [
                    {
                        "key": "tasks_done",
                        "type": "danh_sach",
                        "max_lines": 12,
                        "max_chars_per_line": 95,
                        "source": "general",
                        "show_in_doc": true
                    },
                    {
                        "key": "safety_review",
                        "type": "van_ban",
                        "max_chars": 400,
                        "source_categories": ["safety"],
                        "show_in_doc": false
                    }
                ]
            }
        """.trimIndent()
        val enSchema = TemplateSchema.fromJson(enJson)
        assertEquals("template_en_pure", enSchema.id)
        assertEquals("English Daily Log", enSchema.name)
        assertEquals(4, enSchema.maxPages)
        assertEquals("tuan", enSchema.groupByType)
        assertEquals(2, enSchema.summaryFields.size)
        assertEquals(12, enSchema.summaryFields["tasks_done"]?.maxLines)
        assertFalse(enSchema.summaryFields["safety_review"]?.showInDoc ?: true)

        // 3.3: Hybrid Schema 1: VN root keys, EN summary_fields array
        val hybrid1 = """
            {
                "id": "hybrid_1",
                "ten": "Biểu Mẫu Hỗn Hợp 1",
                "gioi_han_trang": 3,
                "gom_theo": "ngay",
                "summary_fields": [
                    {
                        "key": "cong_viec_chinh",
                        "type": "danh_sach",
                        "max_lines": 8,
                        "source": "thi_cong"
                    }
                ]
            }
        """.trimIndent()
        val h1Schema = TemplateSchema.fromJson(hybrid1)
        assertEquals("Biểu Mẫu Hỗn Hợp 1", h1Schema.name)
        assertEquals(3, h1Schema.maxPages)
        assertEquals(1, h1Schema.summaryFields.size)
        assertEquals(8, h1Schema.summaryFields["cong_viec_chinh"]?.maxLines)

        // 3.4: Hybrid Schema 2: EN root keys, VN truong_tong_hop map, mixed camelCase
        val hybrid2 = """
            {
                "id": "hybrid_2",
                "name": "Hybrid Template 2",
                "maxPages": 1,
                "groupByType": "ca",
                "noteCategories": ["an_toan"],
                "truong_tong_hop": {
                    "an_toan_lao_dong": {
                        "kieu": "van_ban",
                        "toi_da_ky_tu": 150,
                        "muc_nguon": ["an_toan"]
                    }
                }
            }
        """.trimIndent()
        val h2Schema = TemplateSchema.fromJson(hybrid2)
        assertEquals("Hybrid Template 2", h2Schema.name)
        assertEquals(1, h2Schema.maxPages)
        assertEquals("ca", h2Schema.groupByType)
        assertEquals(150, h2Schema.summaryFields["an_toan_lao_dong"]?.maxChars)

        // 3.5: FormSetupService real output parsing
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 0,
                endIndex = 10,
                truoc = "",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Hạng mục",
                loaiGoiY = "danh_sach",
                ma = "HANG_MUC",
                ten = "Hạng mục thi công",
                loai = "danh_sach",
                nguon = "thi_cong"
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
        val entityResult = runBlocking {
            service.applyPlaceholdersAndGenerateTemplate(
                workingDocId = "mock_doc_id",
                templateName = "Biểu Mẫu Tự Sinh M2",
                candidates = candidates,
                accessToken = "mock_token"
            )
        }
        val entity = entityResult.getOrNull()
        assertNotNull(entity)
        val generatedSchema = TemplateSchema.fromJson(entity!!.schemaJson)
        assertEquals("Biểu Mẫu Tự Sinh M2", generatedSchema.name)
        assertTrue(generatedSchema.summaryFields.containsKey("hang_muc"))
        assertEquals("danh_sach", generatedSchema.summaryFields["hang_muc"]?.type)

        // 3.6: Stress edge cases: Malformed / Invalid / Missing fields
        val fallback1 = TemplateSchema.fromJson("NOT_JSON")
        assertEquals("fallback", fallback1.id)
        assertEquals("Mẫu tùy chỉnh", fallback1.name)

        val emptyJson = TemplateSchema.fromJson("{}")
        assertTrue(emptyJson.id.startsWith("custom_"))
        assertEquals("Mẫu tùy chỉnh", emptyJson.name)
        assertEquals(2, emptyJson.maxPages)
        assertTrue(emptyJson.summaryFields.isEmpty())

        val partialJson = TemplateSchema.fromJson("{\"name\": \"Chỉ có tên\"}")
        assertEquals("Chỉ có tên", partialJson.name)
        assertEquals(2, partialJson.maxPages)
    }

    // =============================================================
    // DOMAIN 4: Unified prompt generation format validation
    // =============================================================
    @Test
    fun testUnifiedPromptGenerationFormatValidation() {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 20,
                truoc = "Ngày",
                sau = "tháng",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Thông tin chung",
                loaiGoiY = "ngay",
                ma = "NGAY",
                ten = "Ngày thực hiện",
                loai = "ngay",
                nguon = "nhap_tay"
            ),
            FormCandidate(
                id = 2,
                startIndex = 30,
                endIndex = 50,
                truoc = "1. Thời tiết sáng:",
                sau = "",
                tinHieu = "o_tich",
                tieuDeGanNhat = "Thời tiết",
                loaiGoiY = "o_tich",
                ma = "SANG_NANG",
                ten = "Thời tiết sáng nắng",
                loai = "o_tich",
                nguon = "nhap_tay"
            ),
            FormCandidate(
                id = 3,
                startIndex = 60,
                endIndex = 80,
                truoc = "Ban chỉ huy:",
                sau = "người",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Nhân lực",
                loaiGoiY = "so",
                ma = "BAN_CHI_HUY",
                ten = "Ban chỉ huy công trường",
                loai = "so",
                nguon = "nhap_tay"
            ),
            FormCandidate(
                id = 4,
                startIndex = 90,
                endIndex = 120,
                truoc = "3. Thi công trong ngày:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Thi công",
                loaiGoiY = "danh_sach",
                ma = "CONG_VIEC_THI_CONG",
                ten = "Công việc thi công chính",
                loai = "danh_sach",
                nguon = "thi_cong"
            ),
            FormCandidate(
                id = 5,
                startIndex = 130,
                endIndex = 150,
                truoc = "Ý kiến TVGS:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "Đánh giá",
                loaiGoiY = "van_ban",
                ma = "Y_KIEN_TVGS",
                ten = "Ý kiến Tư vấn giám sát",
                loai = "van_ban",
                nguon = "y_kien_tvgs"
            ),
            FormCandidate(
                id = 6,
                startIndex = 200,
                endIndex = 250,
                truoc = "Chữ ký đơn vị thi công",
                sau = "",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "Khối chữ ký",
                loaiGoiY = "van_ban",
                ma = "CHU_KY",
                ten = "Khối chữ ký",
                loai = "van_ban",
                nguon = "nhap_tay",
                boQua = true
            )
        )

        val prompt = FormLlmDescriber.generateUnifiedPrompt(candidates, "Nhật ký thi công hầm Hải Vân")

        // 4.1 Mandatory Persona and Title
        assertTrue("Must specify construction secretary persona", prompt.contains("thư ký kỹ thuật công trường xây dựng chuyên nghiệp tại Việt Nam"))
        assertTrue("Must include template name", prompt.contains("Nhật ký thi công hầm Hải Vân"))

        // 4.2 Mandatory Rules Sections
        assertTrue("Must contain Section 1: Tone & Polish", prompt.contains("1. BIÊN TẬP MƯỢT MÀ & VĂN PHONG CHUẨN:"))
        assertTrue("Must contain Section 2: Anti-hallucination", prompt.contains("2. TUYỆT ĐỐI KHÔNG BỊA ĐẶT SỐ LIỆU (ANTI-HALLUCINATION):"))
        assertTrue("Must prohibit inventing numbers/workers", prompt.contains("CHỈ sử dụng thông tin có trong bản ghi âm hoặc ghi chú của người dùng"))
        assertTrue("Must specify default fallbacks for missing info", prompt.contains("Nếu mục nào không có thông tin trong ngày: hãy để giá trị mặc định"))
        assertTrue("Must contain Section 3: Page budget constraint", prompt.contains("3. KHỐNG CHẾ DUNG LƯỢNG:"))
        assertTrue("Must contain Section 4: Field list", prompt.contains("4. DANH SÁCH CÁC TRƯỜNG CẦN TỔNG HỢP:"))
        assertTrue("Must contain Section 5: Output format", prompt.contains("5. ĐỊNH DẠNG ĐẦU RA:"))

        // 4.3 Field list validation: all valid {{MA}} present, boQua excluded
        assertTrue("Must include {{NGAY}}", prompt.contains("`{{NGAY}}`"))
        assertTrue("Must include {{SANG_NANG}}", prompt.contains("`{{SANG_NANG}}`"))
        assertTrue("Must include {{BAN_CHI_HUY}}", prompt.contains("`{{BAN_CHI_HUY}}`"))
        assertTrue("Must include {{CONG_VIEC_THI_CONG}}", prompt.contains("`{{CONG_VIEC_THI_CONG}}`"))
        assertTrue("Must include {{Y_KIEN_TVGS}}", prompt.contains("`{{Y_KIEN_TVGS}}`"))
        assertFalse("Must NOT include boQua candidate {{CHU_KY}}", prompt.contains("`{{CHU_KY}}`"))

        // 4.4 Output JSON Section must be strictly parsable JSON
        val section5Idx = prompt.indexOf("5. ĐỊNH DẠNG ĐẦU RA:")
        assertTrue("Must contain Section 5 header", section5Idx != -1)
        val firstBrace = prompt.indexOf('{', section5Idx)
        val lastBrace = prompt.lastIndexOf('}')
        assertTrue("Must contain JSON block in Section 5", firstBrace != -1 && lastBrace > firstBrace)
        val jsonBlock = prompt.substring(firstBrace, lastBrace + 1)
        val sampleJson = JSONObject(jsonBlock)

        // 4.5 Verify 1-to-1 match with field types
        assertTrue(sampleJson.has("ngay"))
        assertEquals("01", sampleJson.getString("ngay"))

        assertTrue(sampleJson.has("sang_nang"))
        assertEquals(false, sampleJson.getBoolean("sang_nang"))

        assertTrue(sampleJson.has("ban_chi_huy"))
        assertEquals("0", sampleJson.getString("ban_chi_huy"))

        assertTrue(sampleJson.has("cong_viec_thi_cong"))
        val listArr = sampleJson.getJSONArray("cong_viec_thi_cong")
        assertTrue(listArr.length() > 0)

        assertTrue(sampleJson.has("y_kien_tvgs"))
        assertEquals("", sampleJson.getString("y_kien_tvgs"))

        assertFalse("Must not have chu_ky in JSON output sample", sampleJson.has("chu_ky"))
    }
}
