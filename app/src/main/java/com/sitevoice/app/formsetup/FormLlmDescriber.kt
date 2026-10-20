package com.sitevoice.app.formsetup

import com.sitevoice.app.llm.LlmClient
import com.sitevoice.app.llm.LlmMessage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Xử lý bước 4 theo docs/10-thiet-lap-form.md:
 * Gửi danh sách ứng viên (đã do code phát hiện) cho LLM (hoặc rule engine)
 * để đặt tên mã placeholder, chọn loại, ghép nguồn dữ liệu và loại bỏ chỗ không cần điền.
 */
class FormLlmDescriber(
    private val llmClient: LlmClient? = null
) {

    suspend fun describeCandidates(candidates: List<FormCandidate>): List<FormCandidate> {
        if (candidates.isEmpty()) return emptyList()

        // 1. Áp dụng tầng quy tắc tất định trước (đảm bảo độ chính xác tối thiểu cho các mẫu công trường phổ biến)
        val described = candidates.map { applyDeterministicRules(it) }.toMutableList()

        // 2. Nếu có LLM client, gọi LLM để hoàn thiện và tinh chỉnh các ứng viên chưa chắc chắn
        if (llmClient != null) {
            try {
                val llmResults = callLlm(candidates)
                for (res in llmResults) {
                    val target = described.find { it.id == res.id }
                    if (target != null && target.doTinCay < 0.99f) {
                        target.ma = res.ma
                        target.ten = res.ten
                        target.loai = res.loai
                        target.nguon = res.nguon
                        target.boQua = res.boQua
                        target.doTinCay = res.doTinCay
                    }
                }
            } catch (e: Exception) {
                // Fallback giữ nguyên kết quả từ tầng quy tắc
            }
        }

        return described
    }

    fun applyDeterministicRules(candidate: FormCandidate): FormCandidate {
        val truoc = candidate.truoc.lowercase()
        val sau = candidate.sau.lowercase()
        val tieuDe = candidate.tieuDeGanNhat.lowercase()

        var ma = candidate.ma
        var ten = candidate.ten
        var loai = candidate.loaiGoiY
        var nguon = "nhap_tay"
        var boQua = false
        var doTinCay = 0.8f

        when {
            // Khối chữ ký ở cuối -> bỏ qua
            tieuDe.contains("chữ ký") || truoc.contains("đơn vị thi công") || truoc.contains("tư vấn giám sát") && candidate.soDongLienTiep > 3 -> {
                boQua = true
                ma = "CHU_KY"
                ten = "Khối chữ ký"
                doTinCay = 0.95f
            }

            // Ngày tháng năm
            truoc.contains("ngày") && (sau.contains("tháng") || sau.contains(".....")) -> {
                ma = "NGAY"
                ten = "Ngày"
                loai = "ngay"
                nguon = "nhap_tay"
                doTinCay = 1.0f
            }
            truoc.contains("tháng") && sau.contains("năm") -> {
                ma = "THANG"
                ten = "Tháng"
                loai = "ngay"
                nguon = "nhap_tay"
                doTinCay = 1.0f
            }
            truoc.contains("năm") -> {
                ma = "NAM"
                ten = "Năm"
                loai = "ngay"
                nguon = "nhap_tay"
                doTinCay = 1.0f
            }

            // Ô tích thời tiết
            candidate.tinHieu == "o_tich" -> {
                loai = "o_tich"
                nguon = "nhap_tay"
                doTinCay = 1.0f
                when {
                    truoc.contains("sáng") && truoc.contains("nắng") -> { ma = "SANG_NANG"; ten = "Sáng: Nắng" }
                    truoc.contains("sáng") && truoc.contains("mưa") -> { ma = "SANG_MUA"; ten = "Sáng: Mưa" }
                    truoc.contains("chiều") && truoc.contains("nắng") -> { ma = "CHIEU_NANG"; ten = "Chiều: Nắng" }
                    truoc.contains("chiều") && truoc.contains("mưa") -> { ma = "CHIEU_MUA"; ten = "Chiều: Mưa" }
                    truoc.contains("tối") && (truoc.contains("bình thường") || truoc.contains("nắng")) -> { ma = "TOI_BINH_THUONG"; ten = "Tối: Bình thường" }
                    truoc.contains("tối") && truoc.contains("mưa") -> { ma = "TOI_MUA"; ten = "Tối: Mưa" }
                    truoc.contains("nắng") -> { ma = "THOI_TIET_NANG"; ten = "Thời tiết: Nắng" }
                    truoc.contains("mưa") -> { ma = "THOI_TIET_MUA"; ten = "Thời tiết: Mưa" }
                    else -> { ma = "O_TICH_${candidate.id}"; ten = "Ô tích lựa chọn" }
                }
            }

            // Nhân lực & máy móc
            truoc.contains("chỉ huy") -> {
                ma = "BAN_CHI_HUY"
                ten = "Ban chỉ huy"
                loai = "so"
                nguon = "nhap_tay"
                doTinCay = 1.0f
            }
            truoc.contains("công nhân") -> {
                ma = "CONG_NHAN"
                ten = "Công nhân"
                loai = "so"
                nguon = "nhap_tay"
                doTinCay = 1.0f
            }
            truoc.contains("máy móc") || tieuDe.contains("máy móc") -> {
                ma = "MAY_MOC"
                ten = "Máy móc thiết bị"
                loai = "van_ban"
                nguon = "nhap_tay"
                doTinCay = 0.95f
            }

            // Các mục ghi chú chính
            tieuDe.contains("nghiệm thu") -> {
                ma = "NGHIEM_THU"
                ten = "Công việc nghiệm thu trong ngày"
                loai = "danh_sach"
                nguon = "nghiem_thu"
                doTinCay = 1.0f
            }
            tieuDe.contains("thi công") -> {
                ma = "CONG_VIEC_THI_CONG"
                ten = "Công việc thi công trong ngày"
                loai = "danh_sach"
                nguon = "thi_cong"
                doTinCay = 1.0f
            }
            tieuDe.contains("an toàn") || tieuDe.contains("vệ sinh") -> {
                ma = "AN_TOAN"
                ten = "An toàn lao động và vệ sinh"
                loai = "danh_sach"
                nguon = "an_toan"
                doTinCay = 1.0f
            }

            // Ý kiến các đơn vị
            tieuDe.contains("chủ đầu tư") || truoc.contains("chủ đầu tư") -> {
                ma = "Y_KIEN_CDT"
                ten = "Ý kiến Chủ đầu tư"
                loai = "van_ban"
                nguon = "y_kien_cdt"
                doTinCay = 0.95f
            }
            tieuDe.contains("tư vấn") || truoc.contains("tư vấn giám sát") -> {
                ma = "Y_KIEN_TVGS"
                ten = "Ý kiến Tư vấn giám sát"
                loai = "van_ban"
                nguon = "y_kien_tvgs"
                doTinCay = 0.95f
            }
            tieuDe.contains("nhà thầu") || truoc.contains("nhà thầu") -> {
                ma = "Y_KIEN_NHA_THAU"
                ten = "Ý kiến Nhà thầu thi công"
                loai = "van_ban"
                nguon = "y_kien_nha_thau"
                doTinCay = 0.95f
            }

            else -> {
                ma = "TRUONG_${candidate.id}"
                ten = if (candidate.tieuDeGanNhat.isNotBlank()) candidate.tieuDeGanNhat else "Trường dữ liệu ${candidate.id}"
                loai = candidate.loaiGoiY
                doTinCay = 0.7f
            }
        }

        return candidate.copy(
            ma = ma,
            ten = ten,
            loai = loai,
            nguon = nguon,
            boQua = boQua,
            doTinCay = doTinCay
        )
    }

    private suspend fun callLlm(candidates: List<FormCandidate>): List<LlmResultItem> {
        val client = llmClient ?: return emptyList()

        val candidateArray = JSONArray()
        for (c in candidates) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("truoc", c.truoc)
                put("sau", c.sau)
                put("tin_hieu", c.tinHieu)
                put("tieu_de", c.tieuDeGanNhat)
                put("so_dong_lien_tiep", c.soDongLienTiep)
                put("loai_goi_y", c.loaiGoiY)
            }
            candidateArray.put(obj)
        }

        val prompt = """
            Bạn giúp thiết lập form báo cáo công trường Việt Nam.
            Dưới đây là danh sách ứng viên chỗ điền do CODE tìm thấy:
            ${candidateArray.toString(2)}
            
            Quy tắc:
            - Trả về đúng từng id.
            - ma viết HOA, không dấu, dạng CONG_VIEC_THI_CONG.
            - loai: "van_ban", "danh_sach", "so", "ngay", "o_tich".
            - nguon: "nhap_tay", "thi_cong", "nghiem_thu", "an_toan", "y_kien_cdt", "y_kien_tvgs", "y_kien_nha_thau".
            - bo_qua: true nếu là khối chữ ký hoặc hoa văn.
            
            Trả về CHỈ JSON theo định dạng:
            {"ket_qua": [{"id": 1, "ma": "NGAY", "ten": "Ngày", "loai": "ngay", "nguon": "nhap_tay", "bo_qua": false, "do_tin_cay": 0.95}]}
        """.trimIndent()

        val messages = listOf(
            LlmMessage("system", "Bạn là chuyên gia phân tích biểu mẫu Google Docs."),
            LlmMessage("user", prompt)
        )

        val response = client.chat(messages, temperature = 0.0f, responseJson = true).getOrNull() ?: return emptyList()

        val list = mutableListOf<LlmResultItem>()
        val json = JSONObject(response)
        val array = json.optJSONArray("ket_qua") ?: return emptyList()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(
                LlmResultItem(
                    id = item.optInt("id"),
                    ma = item.optString("ma").trim().uppercase().replace(" ", "_"),
                    ten = item.optString("ten").trim(),
                    loai = item.optString("loai", "van_ban"),
                    nguon = item.optString("nguon", "nhap_tay"),
                    boQua = item.optBoolean("bo_qua", false),
                    doTinCay = item.optDouble("do_tin_cay", 0.9).toFloat()
                )
            )
        }

        return list
    }

    private data class LlmResultItem(
        val id: Int,
        val ma: String,
        val ten: String,
        val loai: String,
        val nguon: String,
        val boQua: Boolean,
        val doTinCay: Float
    )
}
