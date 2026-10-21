package com.sitevoice.app.template

import android.content.Context
import com.sitevoice.app.data.TemplateDao
import com.sitevoice.app.data.TemplateItemEntity
import kotlinx.coroutines.flow.Flow
import java.io.InputStreamReader

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<TemplateItemEntity>>
    suspend fun getAllTemplatesSync(): List<TemplateItemEntity>
    fun getActiveTemplate(): Flow<TemplateItemEntity?>
    suspend fun getActiveTemplateSync(): TemplateItemEntity?
    fun getTemplateById(id: String): Flow<TemplateItemEntity?>
    suspend fun getTemplateByIdSync(id: String): TemplateItemEntity?
    suspend fun saveTemplate(template: TemplateItemEntity)
    suspend fun deleteTemplate(id: String): Result<Unit>
    suspend fun setActiveTemplate(id: String)
    suspend fun ensureBuiltInTemplatesSeeded()
    suspend fun getActiveSchemaSync(): TemplateSchema
    suspend fun updateTemplatePrompt(id: String, newPrompt: String)
    suspend fun resetTemplatePrompt(id: String)
}

class DefaultTemplateRepository(
    private val context: Context? = null,
    private val templateDao: TemplateDao
) : TemplateRepository {

    companion object {
        const val ID_CONG_TRUONG = "nhat-ky-cong-truong"
        const val ID_CA_NHAN = "nhat-ky-ca-nhan"

        private const val FALLBACK_SCHEMA_CONG_TRUONG = """{
  "id": "nhat-ky-cong-truong",
  "ten": "Nhật ký thi công hằng ngày",
  "gom_theo": "ngay",
  "gioi_han_trang": 2,
  "nhap_tay": {
    "ngay": { "kieu": "ngay" },
    "thang": { "kieu": "so" },
    "s_n": { "kieu": "o_tich", "mac_dinh": true },
    "s_m": { "kieu": "o_tich", "mac_dinh": false },
    "c_n": { "kieu": "o_tich", "mac_dinh": true },
    "c_m": { "kieu": "o_tich", "mac_dinh": false },
    "t_bt": { "kieu": "o_tich", "mac_dinh": true },
    "t_m": { "kieu": "o_tich", "mac_dinh": false },
    "ng_ch": { "kieu": "so", "mac_dinh": "hom_qua" },
    "ng_cn": { "kieu": "so", "mac_dinh": "hom_qua" }
  },
  "muc_ghi_chu": ["thi_cong", "nghiem_thu", "an_toan", "may_moc", "thoi_tiet", "y_kien_cdt", "y_kien_tvgs", "y_kien_nha_thau", "chua_phan_loai"],
  "truong_tong_hop": {
    "cong_viec_thi_cong": { "kieu": "danh_sach", "toi_da_dong": 15, "ky_tu_moi_dong": 100, "muc_nguon": ["thi_cong"] },
    "nghiem_thu": { "kieu": "danh_sach", "toi_da_dong": 6, "ky_tu_moi_dong": 100, "muc_nguon": ["nghiem_thu"] },
    "an_toan": { "kieu": "danh_sach", "toi_da_dong": 3, "ky_tu_moi_dong": 100, "muc_nguon": ["an_toan"] },
    "y_kien_cdt": { "kieu": "van_ban", "toi_da_ky_tu": 200, "muc_nguon": ["y_kien_cdt"] },
    "y_kien_tvgs": { "kieu": "van_ban", "toi_da_ky_tu": 200, "muc_nguon": ["y_kien_tvgs"] },
    "y_kien_nha_thau": { "kieu": "van_ban", "toi_da_ky_tu": 200, "muc_nguon": ["y_kien_nha_thau"] },

    "ngay": { "kieu": "so", "toi_da_dong": 1, "ky_tu_moi_dong": 2, "muc_nguon": ["nhap_tay"] },
    "thang": { "kieu": "so", "toi_da_dong": 1, "ky_tu_moi_dong": 2, "muc_nguon": ["nhap_tay"] },
    "s_n": { "kieu": "o_tich", "toi_da_dong": 1, "ky_tu_moi_dong": 1, "muc_nguon": ["thoi_tiet"] },
    "s_m": { "kieu": "o_tich", "toi_da_dong": 1, "ky_tu_moi_dong": 1, "muc_nguon": ["thoi_tiet"] },
    "c_n": { "kieu": "o_tich", "toi_da_dong": 1, "ky_tu_moi_dong": 1, "muc_nguon": ["thoi_tiet"] },
    "c_m": { "kieu": "o_tich", "toi_da_dong": 1, "ky_tu_moi_dong": 1, "muc_nguon": ["thoi_tiet"] },
    "t_bt": { "kieu": "o_tich", "toi_da_dong": 1, "ky_tu_moi_dong": 1, "muc_nguon": ["thoi_tiet"] },
    "t_m": { "kieu": "o_tich", "toi_da_dong": 1, "ky_tu_moi_dong": 1, "muc_nguon": ["thoi_tiet"] },
    "tong_quan_thoi_tiet": { "kieu": "van_ban", "toi_da_dong": 3, "ky_tu_moi_dong": 75, "muc_nguon": ["thoi_tiet"] },
    "ng_ch": { "kieu": "so", "toi_da_dong": 1, "ky_tu_moi_dong": 3, "muc_nguon": ["nhap_tay"] },
    "ng_cn": { "kieu": "so", "toi_da_dong": 1, "ky_tu_moi_dong": 4, "muc_nguon": ["nhap_tay"] },
    "may_moc": { "kieu": "danh_sach", "toi_da_dong": 5, "ky_tu_moi_dong": 75, "muc_nguon": ["may_moc"] },
    "thi_cong_trong_ngay": { "kieu": "danh_sach", "toi_da_dong": 17, "ky_tu_moi_dong": 75, "muc_nguon": ["thi_cong"] },
    "nghiem_thu_trong_ngay": { "kieu": "danh_sach", "toi_da_dong": 7, "ky_tu_moi_dong": 75, "muc_nguon": ["nghiem_thu"] },
    "an_toan_vs": { "kieu": "danh_sach", "toi_da_dong": 4, "ky_tu_moi_dong": 75, "muc_nguon": ["an_toan"] },
    "yk_cdt": { "kieu": "van_ban", "toi_da_dong": 4, "ky_tu_moi_dong": 75, "muc_nguon": ["y_kien_cdt"] },
    "yk_tvgs": { "kieu": "van_ban", "toi_da_dong": 4, "ky_tu_moi_dong": 75, "muc_nguon": ["y_kien_tvgs"] },
    "yk_nha_thau": { "kieu": "van_ban", "toi_da_dong": 5, "ky_tu_moi_dong": 75, "muc_nguon": ["y_kien_nha_thau"] },
    "chua_phan_loai": { "kieu": "danh_sach", "toi_da_dong": 10, "ky_tu_moi_dong": 160, "hien_trong_doc": false }
  }
}"""

        private const val FALLBACK_PROMPT_CONG_TRUONG = """Bạn là thư ký kỹ thuật công trường xây dựng tại Việt Nam. Nhiệm vụ của bạn là tổng hợp các ghi chú âm thanh trong ngày thành bản nháp báo cáo Nhật ký thi công.
Ngôn ngữ: Tiếng Việt hành chính, cô đọng, súc tích, chuyên nghiệp.

QUY TẮC BẮT BUỘC:
1. QUY TẮC THỜI TIẾT (Ô TÍCH):
   - Mặc định: Sáng nắng (s_n: true, s_m: false), Chiều nắng (c_n: true, c_m: false), Tối bình thường (t_bt: true, t_m: false).
   - Nếu trong ghi chú có nhắc đến TRỜI MƯA ở buổi nào: Buổi đó đảo ngược (mưa: true, nắng/bình thường: false).
   - tong_quan_thoi_tiet: Tối đa 3 dòng (mỗi dòng ≤ 75 ký tự).
2. QUY TẮC NHÂN LỰC:
   - ng_ch: Số người ban chỉ huy (VD: "3").
   - ng_cn: Số công nhân (VD: "28").
3. QUY TẮC TRÌNH BÀY CÔNG VIỆC THI CÔNG (MỤC 3):
   - Tự động gom các việc theo tổ/hạng mục: 3.1 [Tổ/Hạng mục 1], 3.2 [Tổ/Hạng mục 2]... (Ví dụ: 3.1 Điện, 3.2 Nước, 3.3 Kết cấu, 3.4 Hoàn thiện...).
   - Phân loại đúng tổ đội: Nếu ghi chú có câu nói gộp hoặc nhầm việc tổ này vào tổ khác (ví dụ: 'phần nước thì có làm điện tầng 5'), bạn PHẢI phân tách và gom việc điện vào nhóm Điện (3.1 Điện), dưới 3.2 Nước tuyệt đối CHỈ để việc cấp thoát nước.
   - Ví dụ:
     Input: "Về công việc thì phần điện thì có làm điện tầng 8, phần nước thì có làm điện tầng 5, làm trục thoát nước số 11"
     Output "cong_viec_thi_cong": [
       "3.1 Điện",
       "\t- Thi công hệ thống điện tại tầng 8.",
       "\t- Thi công hệ thống điện tại tầng 5.",
       "3.2 Nước",
       "\t- Thi công trục thoát nước số 11."
     ]
   - Dưới mỗi nhóm là các việc chi tiết dạng: "\t- [Nội dung và vị trí]".
   - TỔNG CỘNG cả dòng tiêu đề nhóm 3.x và các dòng gạch đầu dòng tối đa 17 DÒNG. Mỗi dòng ≤ 75 ký tự.
4. GIỚI HẠN CÁC MỤC CÒN LẠI (Tối đa 75 ký tự/dòng):
   - may_moc: Tối đa 5 dòng, gạch đầu dòng "-".
   - nghiem_thu_trong_ngay: Tối đa 7 dòng, gạch đầu dòng "-".
   - an_toan_vs: Tối đa 4 dòng, gạch đầu dòng "-".
   - yk_cdt, yk_tvgs: Tối đa 4 dòng (không có để trống "").
   - yk_nha_thau: Tối đa 5 dòng (không có để trống "").
5. ĐỊNH DẠNG ĐẦU RA:
   - CHỈ trả về JSON thuần túy, không dùng markdown ```json, không giải thích.
   - Trả về JSON PHẲNG (flat JSON) ở cấp cao nhất, KHÔNG lồng vào các object con như "thoi_tiet" hay "nhan_luc".
   - Cấu trúc JSON chuẩn:
   {
     "s_n": true,
     "s_m": false,
     "c_n": false,
     "c_m": true,
     "t_bt": true,
     "t_m": false,
     "tong_quan_thoi_tiet": "Sáng nắng, chiều mưa, tối bình thường.",
     "ng_ch": "8",
     "ng_cn": "10",
     "may_moc": ["- Máy cắt", "- Máy đục"],
     "cong_viec_thi_cong": [
       "3.1 Điện",
       "\t- Thi công hệ thống điện tại tầng 8.",
       "\t- Thi công hệ thống điện tại tầng 5.",
       "3.2 Nước",
       "\t- Thi công trục thoát nước số 11."
     ],
     "nghiem_thu_trong_ngay": [],
     "an_toan_vs": [],
     "yk_cdt": "",
     "yk_tvgs": "",
     "yk_nha_thau": "",
     "chua_phan_loai": []
   }"""

        private const val FALLBACK_SCHEMA_CA_NHAN = """{
  "id": "nhat-ky-ca-nhan",
  "ten": "Nhật ký hằng ngày cá nhân",
  "gom_theo": "ngay",
  "gioi_han_trang": 1,
  "muc_ghi_chu": ["cong_viec", "ton_dong", "suy_nghi", "suc_khoe", "chua_phan_loai"],
  "truong_tong_hop": {
    "cong_viec_hoan_thanh": { "kieu": "danh_sach", "toi_da_dong": 12, "ky_tu_moi_dong": 100, "muc_nguon": ["cong_viec"] },
    "viec_ton_dong": { "kieu": "danh_sach", "toi_da_dong": 8, "ky_tu_moi_dong": 100, "muc_nguon": ["ton_dong"] },
    "suy_nghi_bai_hoc": { "kieu": "danh_sach", "toi_da_dong": 6, "ky_tu_moi_dong": 100, "muc_nguon": ["suy_nghi"] },
    "suc_khoe_tam_trang": { "kieu": "van_ban", "toi_da_ky_tu": 150, "muc_nguon": ["suc_khoe"] },
    "chua_phan_loai": { "kieu": "danh_sach", "toi_da_dong": 10, "ky_tu_moi_dong": 160, "hien_trong_doc": false }
  }
}"""

        private const val FALLBACK_PROMPT_CA_NHAN = """Bạn là trợ lý cá nhân tinh tế, trung thực và chu đáo.
Từ các ghi chú được cung cấp trong ngày của người dùng, hãy viết bản tổng hợp Nhật ký cá nhân hằng ngày bằng tiếng Việt, mạch lạc, súc tích và chân thực để người dùng duyệt.

Quy tắc bắt buộc:
1. CHỈ dùng thông tin có trong ghi chú. Tuyệt đối KHÔNG suy đoán hay tự thêm thắt tình tiết.
2. Mục nào không có dữ liệu: trả về mảng rỗng [] hoặc chuỗi rỗng "".
3. Tuân thủ giới hạn số dòng và ký tự:
   - "cong_viec_hoan_thanh": mảng các việc đã hoàn thành trong ngày (tối đa 12 dòng, mỗi dòng ≤ 100 ký tự)
   - "viec_ton_dong": mảng các việc còn dang dở hoặc cần xử lý ngày mai (tối đa 8 dòng, mỗi dòng ≤ 100 ký tự)
   - "suy_nghi_bai_hoc": mảng các suy nghĩ, ý tưởng, cảm nhận hoặc bài học rút ra (tối đa 6 dòng, mỗi dòng ≤ 100 ký tự)
   - "suc_khoe_tam_trang": chuỗi ngắn gọn ghi nhận tâm trạng, sức khỏe, thể chất (≤ 150 ký tự)
   - "chua_phan_loai": mảng ghi chú chưa phân loại
4. Trả về CHỈ JSON theo đúng định dạng được yêu cầu, không kèm giải thích hay bọc markdown."""
    }

    override fun getAllTemplates(): Flow<List<TemplateItemEntity>> {
        return templateDao.getAllTemplates()
    }

    override suspend fun getAllTemplatesSync(): List<TemplateItemEntity> {
        return templateDao.getAllTemplatesSync()
    }

    override fun getActiveTemplate(): Flow<TemplateItemEntity?> {
        return templateDao.getActiveTemplate()
    }

    override suspend fun getActiveTemplateSync(): TemplateItemEntity? {
        return templateDao.getActiveTemplateSync()
    }

    override fun getTemplateById(id: String): Flow<TemplateItemEntity?> {
        return templateDao.getTemplateById(id)
    }

    override suspend fun getTemplateByIdSync(id: String): TemplateItemEntity? {
        return templateDao.getTemplateByIdSync(id)
    }

    override suspend fun saveTemplate(template: TemplateItemEntity) {
        templateDao.insertOrUpdate(template)
    }

    override suspend fun deleteTemplate(id: String): Result<Unit> {
        val existing = templateDao.getTemplateByIdSync(id) ?: return Result.failure(IllegalArgumentException("Mẫu không tồn tại"))
        if (existing.isBuiltIn) {
            return Result.failure(IllegalStateException("Không thể xóa mẫu mặc định của hệ thống"))
        }
        templateDao.deleteCustomTemplate(id)
        return Result.success(Unit)
    }

    override suspend fun setActiveTemplate(id: String) {
        templateDao.setActiveTemplate(id)
    }

    override suspend fun ensureBuiltInTemplatesSeeded() {
        val schemaCongTruong = readAssetFile("templates/$ID_CONG_TRUONG/schema.json") ?: FALLBACK_SCHEMA_CONG_TRUONG
        val promptCongTruong = readAssetFile("templates/$ID_CONG_TRUONG/prompt.md") ?: FALLBACK_PROMPT_CONG_TRUONG

        val schemaCaNhan = readAssetFile("templates/$ID_CA_NHAN/schema.json") ?: FALLBACK_SCHEMA_CA_NHAN
        val promptCaNhan = readAssetFile("templates/$ID_CA_NHAN/prompt.md") ?: FALLBACK_PROMPT_CA_NHAN

        val count = templateDao.countTemplates()
        if (count > 0) {
            // Tự động nâng cấp mẫu mặc định nếu prompt hoặc schema trong code/asset mới hơn
            val currentCt = templateDao.getTemplateByIdSync(ID_CONG_TRUONG)
            if (currentCt != null && currentCt.isBuiltIn && (currentCt.systemPrompt != promptCongTruong || currentCt.schemaJson != schemaCongTruong)) {
                templateDao.insertOrUpdate(
                    currentCt.copy(
                        schemaJson = schemaCongTruong,
                        systemPrompt = promptCongTruong,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            return
        }

        val congTruongTemplate = TemplateItemEntity(
            id = ID_CONG_TRUONG,
            name = "Nhật ký thi công công trường",
            description = "Báo cáo công việc, nghiệm thu, an toàn, vật tư và nhân lực tại công trường",
            docTemplateUrl = "",
            systemPrompt = promptCongTruong,
            schemaJson = schemaCongTruong,
            isBuiltIn = true,
            isActive = true
        )

        val caNhanTemplate = TemplateItemEntity(
            id = ID_CA_NHAN,
            name = "Nhật ký hằng ngày cá nhân",
            description = "Ghi chép công việc cá nhân, bài học, việc tồn đọng và to-do hằng ngày",
            docTemplateUrl = "",
            systemPrompt = promptCaNhan,
            schemaJson = schemaCaNhan,
            isBuiltIn = true,
            isActive = false
        )

        templateDao.insertOrUpdate(congTruongTemplate)
        templateDao.insertOrUpdate(caNhanTemplate)
    }

    override suspend fun updateTemplatePrompt(id: String, newPrompt: String) {
        val existing = templateDao.getTemplateByIdSync(id) ?: return
        templateDao.insertOrUpdate(existing.copy(systemPrompt = newPrompt.trim(), updatedAt = System.currentTimeMillis()))
    }

    override suspend fun resetTemplatePrompt(id: String) {
        val existing = templateDao.getTemplateByIdSync(id) ?: return
        val defaultPrompt = when (id) {
            ID_CONG_TRUONG -> readAssetFile("templates/$ID_CONG_TRUONG/prompt.md") ?: FALLBACK_PROMPT_CONG_TRUONG
            ID_CA_NHAN -> readAssetFile("templates/$ID_CA_NHAN/prompt.md") ?: FALLBACK_PROMPT_CA_NHAN
            else -> existing.systemPrompt
        }
        templateDao.insertOrUpdate(existing.copy(systemPrompt = defaultPrompt.trim(), updatedAt = System.currentTimeMillis()))
    }

    override suspend fun getActiveSchemaSync(): TemplateSchema {
        val active = templateDao.getActiveTemplateSync()
        return if (active != null) {
            TemplateSchema.fromJson(active.schemaJson)
        } else {
            val fallbackJson = readAssetFile("templates/$ID_CONG_TRUONG/schema.json") ?: FALLBACK_SCHEMA_CONG_TRUONG
            TemplateSchema.fromJson(fallbackJson)
        }
    }

    private fun readAssetFile(path: String): String? {
        return try {
            context?.assets?.open(path)?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }
}
