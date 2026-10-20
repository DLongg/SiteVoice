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
  "muc_ghi_chu": ["thi_cong", "nghiem_thu", "an_toan", "y_kien_cdt", "y_kien_tvgs", "y_kien_nha_thau", "chua_phan_loai"],
  "truong_tong_hop": {
    "cong_viec_thi_cong": { "kieu": "danh_sach", "toi_da_dong": 15, "ky_tu_moi_dong": 100, "muc_nguon": ["thi_cong"] },
    "nghiem_thu": { "kieu": "danh_sach", "toi_da_dong": 6, "ky_tu_moi_dong": 100, "muc_nguon": ["nghiem_thu"] },
    "an_toan": { "kieu": "danh_sach", "toi_da_dong": 3, "ky_tu_moi_dong": 100, "muc_nguon": ["an_toan"] },
    "y_kien_cdt": { "kieu": "van_ban", "toi_da_ky_tu": 200, "muc_nguon": ["y_kien_cdt"] },
    "y_kien_tvgs": { "kieu": "van_ban", "toi_da_ky_tu": 200, "muc_nguon": ["y_kien_tvgs"] },
    "y_kien_nha_thau": { "kieu": "van_ban", "toi_da_ky_tu": 200, "muc_nguon": ["y_kien_nha_thau"] },
    "chua_phan_loai": { "kieu": "danh_sach", "toi_da_dong": 10, "ky_tu_moi_dong": 160, "hien_trong_doc": false }
  }
}"""

        private const val FALLBACK_PROMPT_CONG_TRUONG = """Bạn là thư ký công trường xây dựng chuyên nghiệp tại Việt Nam.
Từ các ghi chú được cung cấp trong ngày, hãy viết bản tổng hợp Nhật ký thi công hằng ngày bằng tiếng Việt, văn phong hành chính kỹ thuật ngắn gọn, súc tích để người dùng duyệt.

Quy tắc bắt buộc:
1. CHỈ dùng thông tin có trong ghi chú. Tuyệt đối KHÔNG bịa, KHÔNG suy đoán số liệu.
2. Giữ nguyên số liệu, vị trí, tên hạng mục, tên đơn vị.
3. Mục nào không có dữ liệu: trả về mảng rỗng [] hoặc chuỗi rỗng "".
4. Gộp các ý trùng lặp, ưu tiên việc quan trọng.
5. Ghi chú thuộc "chua_phan_loai": KHÔNG tự gán vào mục khác.
6. Tuân thủ nghiêm ngặt giới hạn số dòng và số ký tự.
7. Trả về CHỈ JSON theo đúng định dạng được yêu cầu, không kèm giải thích hay bọc markdown."""

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
        val count = templateDao.countTemplates()
        if (count > 0) return

        val schemaCongTruong = readAssetFile("templates/$ID_CONG_TRUONG/schema.json") ?: FALLBACK_SCHEMA_CONG_TRUONG
        val promptCongTruong = readAssetFile("templates/$ID_CONG_TRUONG/prompt.md") ?: FALLBACK_PROMPT_CONG_TRUONG

        val schemaCaNhan = readAssetFile("templates/$ID_CA_NHAN/schema.json") ?: FALLBACK_SCHEMA_CA_NHAN
        val promptCaNhan = readAssetFile("templates/$ID_CA_NHAN/prompt.md") ?: FALLBACK_PROMPT_CA_NHAN

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
