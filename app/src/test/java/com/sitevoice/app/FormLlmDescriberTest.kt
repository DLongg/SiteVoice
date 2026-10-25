package com.sitevoice.app

import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormLlmDescriber
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormLlmDescriberTest {

    @Test
    fun testDeterministicRulesIdentifyAllStandardFields() = runBlocking {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 15,
                truoc = "Hà Nội, ngày",
                sau = "tháng",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "",
                loaiGoiY = "ngay"
            ),
            FormCandidate(
                id = 2,
                startIndex = 20,
                endIndex = 25,
                truoc = "tháng",
                sau = "năm 2026",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "",
                loaiGoiY = "ngay"
            ),
            FormCandidate(
                id = 3,
                startIndex = 60,
                endIndex = 61,
                truoc = "- Sáng: trời nắng",
                sau = "trời mưa",
                tinHieu = "o_tich",
                tieuDeGanNhat = "1. Thời tiết:",
                loaiGoiY = "o_tich"
            ),
            FormCandidate(
                id = 4,
                startIndex = 70,
                endIndex = 71,
                truoc = "- Sáng: trời mưa",
                sau = "- Chiều:",
                tinHieu = "o_tich",
                tieuDeGanNhat = "1. Thời tiết:",
                loaiGoiY = "o_tich"
            ),
            FormCandidate(
                id = 5,
                startIndex = 100,
                endIndex = 105,
                truoc = "- Ban chỉ huy công trường:",
                sau = "người",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Lực lượng thi công:",
                loaiGoiY = "so"
            ),
            FormCandidate(
                id = 6,
                startIndex = 120,
                endIndex = 125,
                truoc = "- Công nhân:",
                sau = "người",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Lực lượng thi công:",
                loaiGoiY = "so"
            ),
            FormCandidate(
                id = 7,
                startIndex = 150,
                endIndex = 155,
                truoc = "* Máy móc – thiết bị thi công chính:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Lực lượng thi công:",
                loaiGoiY = "van_ban"
            ),
            FormCandidate(
                id = 8,
                startIndex = 200,
                endIndex = 220,
                truoc = "3. Công việc thi công trong ngày:",
                sau = "4. Công việc nghiệm thu trong ngày:",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "3. Công việc thi công trong ngày:",
                soDongLienTiep = 5,
                loaiGoiY = "danh_sach"
            ),
            FormCandidate(
                id = 9,
                startIndex = 250,
                endIndex = 270,
                truoc = "4. Công việc nghiệm thu trong ngày:",
                sau = "5. An toàn lao động...",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "4. Công việc nghiệm thu trong ngày:",
                soDongLienTiep = 3,
                loaiGoiY = "danh_sach"
            ),
            FormCandidate(
                id = 10,
                startIndex = 300,
                endIndex = 320,
                truoc = "5. An toàn lao động và vệ sinh môi trường:",
                sau = "6. Ý kiến của các đơn vị:",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "5. An toàn lao động và vệ sinh môi trường:",
                soDongLienTiep = 2,
                loaiGoiY = "danh_sach"
            )
        )

        val describer = FormLlmDescriber()
        val result = describer.describeCandidates(candidates)

        assertEquals("NGAY", result[0].ma)
        assertEquals("THANG", result[1].ma)
        assertEquals("SANG_NANG", result[2].ma)
        assertEquals("SANG_MUA", result[3].ma)
        assertEquals("BAN_CHI_HUY", result[4].ma)
        assertEquals("CONG_NHAN", result[5].ma)
        assertEquals("MAY_MOC", result[6].ma)
        assertEquals("CONG_VIEC_THI_CONG", result[7].ma)
        assertEquals("thi_cong", result[7].nguon)
        assertEquals("NGHIEM_THU", result[8].ma)
        assertEquals("nghiem_thu", result[8].nguon)
        assertEquals("AN_TOAN", result[9].ma)
        assertEquals("an_toan", result[9].nguon)

        // Không trường nào bị bỏ qua
        assertTrue(result.all { !it.boQua })
    }

    @Test
    fun testDeduplicateCandidateCodes() {
        val input = listOf(
            FormCandidate(id = 1, startIndex = 0, endIndex = 5, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "CONG_VIEC"),
            FormCandidate(id = 2, startIndex = 10, endIndex = 15, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "CONG_VIEC"),
            FormCandidate(id = 3, startIndex = 20, endIndex = 25, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "CONG_VIEC"),
            FormCandidate(id = 4, startIndex = 30, endIndex = 35, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "CHU_KY", boQua = true),
            FormCandidate(id = 5, startIndex = 40, endIndex = 45, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "CHU_KY", boQua = true),
            FormCandidate(id = 6, startIndex = 50, endIndex = 55, truoc = "", sau = "", tinHieu = "dong_cham", tieuDeGanNhat = "", loaiGoiY = "van_ban", ma = "")
        )

        val deduplicated = FormLlmDescriber.deduplicateCandidateCodes(input)

        assertEquals("CONG_VIEC", deduplicated[0].ma)
        assertEquals("CONG_VIEC_2", deduplicated[1].ma)
        assertEquals("CONG_VIEC_3", deduplicated[2].ma)
        assertEquals("CHU_KY", deduplicated[3].ma)
        assertEquals("CHU_KY", deduplicated[4].ma)
        assertEquals("TRUONG_6", deduplicated[5].ma)
    }

    @Test
    fun testDescribeCandidatesAppliesDeduplication() = runBlocking {
        // Hai vị trí đều có tiêu đề hoặc truoc chứa "máy móc"
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 20,
                truoc = "Máy móc phục vụ đào đất:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Thiết bị:",
                loaiGoiY = "van_ban"
            ),
            FormCandidate(
                id = 2,
                startIndex = 30,
                endIndex = 40,
                truoc = "Máy móc phục vụ đổ bê tông:",
                sau = "",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "2. Thiết bị:",
                loaiGoiY = "van_ban"
            )
        )

        val describer = FormLlmDescriber()
        val result = describer.describeCandidates(candidates)

        assertEquals("MAY_MOC", result[0].ma)
        assertEquals("MAY_MOC_2", result[1].ma)
    }

    @Test
    fun testGenerateUnifiedPromptCreatesComprehensivePrompt() {
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 20,
                truoc = "Hà Nội, ngày",
                sau = "tháng",
                tinHieu = "dong_cham",
                tieuDeGanNhat = "",
                loaiGoiY = "ngay",
                ma = "NGAY",
                ten = "Ngày",
                loai = "ngay",
                nguon = "nhap_tay"
            ),
            FormCandidate(
                id = 2,
                startIndex = 30,
                endIndex = 50,
                truoc = "3. Công việc thi công:",
                sau = "",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "3. Công việc thi công:",
                loaiGoiY = "danh_sach",
                ma = "CONG_VIEC_THI_CONG",
                ten = "Công việc thi công",
                loai = "danh_sach",
                nguon = "thi_cong"
            ),
            FormCandidate(
                id = 3,
                startIndex = 60,
                endIndex = 70,
                truoc = "Khối chữ ký:",
                sau = "",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "Chữ ký",
                loaiGoiY = "van_ban",
                ma = "CHU_KY",
                ten = "Khối chữ ký",
                loai = "van_ban",
                nguon = "nhap_tay",
                boQua = true
            )
        )

        val prompt = FormLlmDescriber.generateUnifiedPrompt(candidates, "Nhật ký thi công Biểu mẫu A")

        // 1. Kiểm tra vai trò & tên mẫu
        assertTrue(prompt.contains("thư ký kỹ thuật công trường xây dựng chuyên nghiệp"))
        assertTrue(prompt.contains("Nhật ký thi công Biểu mẫu A"))

        // 2. Kiểm tra các quy tắc bắt buộc
        assertTrue(prompt.contains("BIÊN TẬP MƯỢT MÀ & VĂN PHONG CHUẨN"))
        assertTrue(prompt.contains("TUYỆT ĐỐI KHÔNG BỊA ĐẶT SỐ LIỆU (ANTI-HALLUCINATION)"))
        assertTrue(prompt.contains("KHỐNG CHẾ DUNG LƯỢNG"))

        // 3. Kiểm tra các trường được kích hoạt
        assertTrue(prompt.contains("`{{NGAY}}`"))
        assertTrue(prompt.contains("`{{CONG_VIEC_THI_CONG}}`"))
        // Trường boQua = true không được xuất hiện trong prompt
        assertFalse(prompt.contains("`{{CHU_KY}}`"))

        // 4. Kiểm tra JSON mẫu ở cuối
        assertTrue(prompt.contains("\"ngay\":"))
        assertTrue(prompt.contains("\"cong_viec_thi_cong\":"))
        assertFalse(prompt.contains("\"chu_ky\":"))

        // Bóc tách JSON mẫu ra để kiểm tra tính hợp lệ
        val jsonStartIndex = prompt.lastIndexOf('{')
        val jsonEndIndex = prompt.lastIndexOf('}')
        assertTrue(jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex)
        val jsonSubstr = prompt.substring(jsonStartIndex, jsonEndIndex + 1)
        val parsedJson = JSONObject(jsonSubstr)
        assertTrue(parsedJson.has("ngay"))
        assertTrue(parsedJson.has("cong_viec_thi_cong"))
        assertFalse(parsedJson.has("chu_ky"))
    }

    @Test
    fun testCallLlmWithMarkdownFencedJson() = runBlocking {
        val fakeLlmClient = object : com.sitevoice.app.llm.LlmClient {
            override suspend fun chat(
                messages: List<com.sitevoice.app.llm.LlmMessage>,
                temperature: Float,
                responseJson: Boolean
            ): Result<String> {
                val jsonPayload = """
                    ```json
                    {
                      "ket_qua": [
                        {
                          "id": 1,
                          "ma": "CONG_VIEC_THI_CONG",
                          "ten": "Công việc thi công",
                          "loai": "danh_sach",
                          "nguon": "thi_cong",
                          "bo_qua": false,
                          "do_tin_cay": 0.98
                        }
                      ]
                    }
                    ```
                """.trimIndent()
                return Result.success(jsonPayload)
            }
        }

        val describer = FormLlmDescriber(fakeLlmClient)
        val candidates = listOf(
            FormCandidate(
                id = 1,
                startIndex = 10,
                endIndex = 20,
                truoc = "Mục chung:",
                sau = "",
                tinHieu = "doan_trong",
                tieuDeGanNhat = "Mục chung",
                loaiGoiY = "van_ban"
            )
        )

        val result = describer.describeCandidates(candidates)
        assertEquals(1, result.size)
        assertEquals("CONG_VIEC_THI_CONG", result[0].ma)
        assertEquals("thi_cong", result[0].nguon)
    }

    @Test
    fun testPriorityRulesOpinionAndEquipmentBeforeThiCong() {
        val describer = FormLlmDescriber()

        // 1. Nhà thầu thi công xây dựng -> Y_KIEN_NHA_THAU, not CONG_VIEC_THI_CONG
        val c1 = describer.applyDeterministicRules(
            FormCandidate(
                id = 1, startIndex = 0, endIndex = 10, truoc = "c. Nhà thầu thi công xây dựng:", sau = "",
                tinHieu = "dong_cham", tieuDeGanNhat = "6. Ý kiến của các đơn vị:", loaiGoiY = "van_ban"
            )
        )
        assertEquals("Y_KIEN_NHA_THAU", c1.ma)
        assertEquals("y_kien_nha_thau", c1.nguon)

        // 2. Máy móc thiết bị thi công -> MAY_MOC, not CONG_VIEC_THI_CONG
        val c2 = describer.applyDeterministicRules(
            FormCandidate(
                id = 2, startIndex = 20, endIndex = 30, truoc = "* Máy móc thiết bị thi công chính:", sau = "",
                tinHieu = "dong_cham", tieuDeGanNhat = "2. Lực lượng thi công", loaiGoiY = "van_ban"
            )
        )
        assertEquals("MAY_MOC", c2.ma)
        assertEquals("nhap_tay", c2.nguon)

        // 3. Tư vấn giám sát -> Y_KIEN_TVGS
        val c3 = describer.applyDeterministicRules(
            FormCandidate(
                id = 3, startIndex = 40, endIndex = 50, truoc = "b. Tư vấn giám sát:", sau = "",
                tinHieu = "dong_cham", tieuDeGanNhat = "6. Ý kiến của các đơn vị:", loaiGoiY = "van_ban"
            )
        )
        assertEquals("Y_KIEN_TVGS", c3.ma)
        assertEquals("y_kien_tvgs", c3.nguon)

        // 4. Chủ đầu tư -> Y_KIEN_CDT
        val c4 = describer.applyDeterministicRules(
            FormCandidate(
                id = 4, startIndex = 60, endIndex = 70, truoc = "a. Chủ đầu tư:", sau = "",
                tinHieu = "dong_cham", tieuDeGanNhat = "6. Ý kiến của các đơn vị:", loaiGoiY = "van_ban"
            )
        )
        assertEquals("Y_KIEN_CDT", c4.ma)
        assertEquals("y_kien_cdt", c4.nguon)

        // 5. Công việc thi công trong ngày -> CONG_VIEC_THI_CONG
        val c5 = describer.applyDeterministicRules(
            FormCandidate(
                id = 5, startIndex = 80, endIndex = 90, truoc = "3. Công việc thi công trong ngày:", sau = "",
                tinHieu = "doan_trong", tieuDeGanNhat = "3. Công việc thi công trong ngày:", loaiGoiY = "danh_sach"
            )
        )
        assertEquals("CONG_VIEC_THI_CONG", c5.ma)
        assertEquals("thi_cong", c5.nguon)
    }
}
