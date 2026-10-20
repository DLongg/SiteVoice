package com.sitevoice.app

import com.sitevoice.app.data.TemplateDao
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.template.DefaultTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTemplateDao : TemplateDao {
    private val templates = mutableMapOf<String, TemplateItemEntity>()
    private val flow = MutableStateFlow<List<TemplateItemEntity>>(emptyList())

    private fun updateFlow() {
        flow.value = templates.values.sortedWith(
            compareByDescending<TemplateItemEntity> { it.isBuiltIn }
                .thenBy { it.createdAt }
        )
    }

    override fun getAllTemplates(): Flow<List<TemplateItemEntity>> = flow

    override suspend fun getAllTemplatesSync(): List<TemplateItemEntity> {
        return flow.value
    }

    override fun getTemplateById(id: String): Flow<TemplateItemEntity?> {
        return flow.map { list -> list.find { it.id == id } }
    }

    override suspend fun getTemplateByIdSync(id: String): TemplateItemEntity? {
        return templates[id]
    }

    override fun getActiveTemplate(): Flow<TemplateItemEntity?> {
        return flow.map { list -> list.find { it.isActive } }
    }

    override suspend fun getActiveTemplateSync(): TemplateItemEntity? {
        return templates.values.find { it.isActive }
    }

    override suspend fun insertOrUpdate(template: TemplateItemEntity) {
        templates[template.id] = template
        updateFlow()
    }

    override suspend fun update(template: TemplateItemEntity) {
        templates[template.id] = template
        updateFlow()
    }

    override suspend fun deleteCustomTemplate(id: String): Int {
        val existing = templates[id]
        return if (existing != null && !existing.isBuiltIn) {
            templates.remove(id)
            updateFlow()
            1
        } else {
            0
        }
    }

    override suspend fun deactivateAll() {
        templates.forEach { (k, v) ->
            templates[k] = v.copy(isActive = false)
        }
        updateFlow()
    }

    override suspend fun activateTemplate(id: String, updatedAt: Long) {
        templates[id]?.let {
            templates[id] = it.copy(isActive = true, updatedAt = updatedAt)
        }
        updateFlow()
    }

    override suspend fun countTemplates(): Int {
        return templates.size
    }
}

class TemplateRepositoryTest {

    private lateinit var fakeDao: FakeTemplateDao
    private lateinit var repository: DefaultTemplateRepository

    @Before
    fun setUp() {
        fakeDao = FakeTemplateDao()
        repository = DefaultTemplateRepository(context = null, templateDao = fakeDao)
    }

    @Test
    fun testEnsureBuiltInTemplatesSeeded() = runBlocking {
        assertEquals(0, fakeDao.countTemplates())

        repository.ensureBuiltInTemplatesSeeded()

        assertEquals(2, fakeDao.countTemplates())

        val congTruong = repository.getTemplateByIdSync(DefaultTemplateRepository.ID_CONG_TRUONG)
        assertNotNull(congTruong)
        assertTrue(congTruong!!.isBuiltIn)
        assertTrue(congTruong.isActive)

        val caNhan = repository.getTemplateByIdSync(DefaultTemplateRepository.ID_CA_NHAN)
        assertNotNull(caNhan)
        assertTrue(caNhan!!.isBuiltIn)
        assertFalse(caNhan.isActive)
    }

    @Test
    fun testSwitchActiveTemplate() = runBlocking {
        repository.ensureBuiltInTemplatesSeeded()

        var active = repository.getActiveTemplateSync()
        assertEquals(DefaultTemplateRepository.ID_CONG_TRUONG, active?.id)

        repository.setActiveTemplate(DefaultTemplateRepository.ID_CA_NHAN)

        active = repository.getActiveTemplateSync()
        assertEquals(DefaultTemplateRepository.ID_CA_NHAN, active?.id)

        val schema = repository.getActiveSchemaSync()
        assertEquals(DefaultTemplateRepository.ID_CA_NHAN, schema.id)
        assertTrue(schema.summaryFields.containsKey("cong_viec_hoan_thanh"))
    }

    @Test
    fun testCreateCustomTemplateAndAdjustBudget() = runBlocking {
        repository.ensureBuiltInTemplatesSeeded()

        val customSchemaJson = """{
            "id": "nhat-ky-hoc-tap",
            "ten": "Nhật ký học tập",
            "gom_theo": "ngay",
            "gioi_han_trang": 1,
            "muc_ghi_chu": ["bai_hoc", "bai_tap", "chua_phan_loai"],
            "truong_tong_hop": {
                "bai_hoc": { "kieu": "danh_sach", "toi_da_dong": 10, "ky_tu_moi_dong": 80 },
                "bai_tap": { "kieu": "danh_sach", "toi_da_dong": 5, "ky_tu_moi_dong": 80 }
            }
        }"""

        val customTemplate = TemplateItemEntity(
            id = "custom_study_01",
            name = "Nhật ký học tập",
            description = "Theo dõi bài giảng và bài tập",
            docTemplateUrl = "https://docs.google.com/document/d/xyz",
            systemPrompt = "Bạn là trợ lý học tập...",
            schemaJson = customSchemaJson,
            isBuiltIn = false,
            isActive = false
        )

        repository.saveTemplate(customTemplate)
        assertEquals(3, fakeDao.countTemplates())

        // User căn chỉnh tăng ngân sách dòng từ 10 lên 15 dòng
        val adjustedSchemaJson = customSchemaJson.replace("\"toi_da_dong\": 10", "\"toi_da_dong\": 15")
        val updatedTemplate = customTemplate.copy(schemaJson = adjustedSchemaJson)
        repository.saveTemplate(updatedTemplate)

        val retrieved = repository.getTemplateByIdSync("custom_study_01")
        assertNotNull(retrieved)
        assertTrue(retrieved!!.schemaJson.contains("\"toi_da_dong\": 15"))

        // Kích hoạt mẫu tùy chỉnh
        repository.setActiveTemplate("custom_study_01")
        val active = repository.getActiveTemplateSync()
        assertEquals("custom_study_01", active?.id)
    }

    @Test
    fun testDeleteTemplateProtection() = runBlocking {
        repository.ensureBuiltInTemplatesSeeded()

        // Không cho phép xóa mẫu mặc định
        val deleteBuiltInResult = repository.deleteTemplate(DefaultTemplateRepository.ID_CONG_TRUONG)
        assertTrue(deleteBuiltInResult.isFailure)
        assertEquals(2, fakeDao.countTemplates())

        // Thêm mẫu custom và xóa thành công
        val customTemplate = TemplateItemEntity(
            id = "custom_test_temp",
            name = "Mẫu thử",
            description = "Mô tả",
            systemPrompt = "Prompt",
            schemaJson = "{}",
            isBuiltIn = false
        )
        repository.saveTemplate(customTemplate)
        assertEquals(3, fakeDao.countTemplates())

        val deleteCustomResult = repository.deleteTemplate("custom_test_temp")
        assertTrue(deleteCustomResult.isSuccess)
        assertEquals(2, fakeDao.countTemplates())
    }
}
