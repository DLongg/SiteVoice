package com.sitevoice.app

import com.sitevoice.app.data.DailyReportDao
import com.sitevoice.app.data.DailyReportEntity
import com.sitevoice.app.data.DefaultDailyReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class FakeDailyReportDao : DailyReportDao {
    private val map = mutableMapOf<String, DailyReportEntity>()
    private val flow = MutableStateFlow<Map<String, DailyReportEntity>>(emptyMap())

    override suspend fun insertOrUpdate(report: DailyReportEntity) {
        map[report.date] = report
        flow.value = map.toMap()
    }

    override fun getByDate(date: String): Flow<DailyReportEntity?> {
        return flow.map { it[date] }
    }

    override suspend fun getByDateSync(date: String): DailyReportEntity? {
        return map[date]
    }

    override suspend fun updateStatus(date: String, status: String, updatedAt: Long) {
        map[date]?.let {
            map[date] = it.copy(status = status, updatedAt = updatedAt)
            flow.value = map.toMap()
        }
    }

    override suspend fun updateDocUrl(date: String, docUrl: String, status: String, updatedAt: Long) {
        map[date]?.let {
            map[date] = it.copy(docUrl = docUrl, status = status, updatedAt = updatedAt)
            flow.value = map.toMap()
        }
    }
}

class DailyReportRepositoryTest {

    private lateinit var fakeDao: FakeDailyReportDao
    private lateinit var repository: DefaultDailyReportRepository

    @Before
    fun setUp() {
        fakeDao = FakeDailyReportDao()
        repository = DefaultDailyReportRepository(fakeDao)
    }

    @Test
    fun testDraftAndApproveFlow() {
        runBlocking {
            val report = DailyReportEntity(
                date = "2026-10-20",
                templateId = "nhat-ky-cong-truong",
                contentJson = """{"cong_viec_thi_cong": ["Đổ bê tông sàn tầng 2 45m3"]}""",
                status = "DRAFT"
            )

            repository.saveReport(report)
            val draft = repository.getReportSync("2026-10-20")
            assertNotNull(draft)
            assertEquals("DRAFT", draft?.status)

            // Duyệt theo quyết định 0006
            repository.approveReport("2026-10-20")
            val approved = repository.getReportSync("2026-10-20")
            assertEquals("APPROVED", approved?.status)

            // Xuất Doc thành công
            repository.markExported("2026-10-20", "https://docs.google.com/document/d/123/edit")
            val exported = repository.getReportSync("2026-10-20")
            assertEquals("EXPORTED", exported?.status)
            assertEquals("https://docs.google.com/document/d/123/edit", exported?.docUrl)
        }
    }
}
