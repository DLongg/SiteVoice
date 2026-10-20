package com.sitevoice.app

import com.sitevoice.app.data.DailyInputDao
import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.DefaultDailyInputRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class FakeDailyInputDao : DailyInputDao {
    private val map = mutableMapOf<String, DailyInputEntity>()
    private val flow = MutableStateFlow<Map<String, DailyInputEntity>>(emptyMap())

    override suspend fun insertOrUpdate(input: DailyInputEntity) {
        map[input.date] = input
        flow.value = map.toMap()
    }

    override fun getByDate(date: String): Flow<DailyInputEntity?> {
        return flow.map { it[date] }
    }

    override suspend fun getByDateSync(date: String): DailyInputEntity? {
        return map[date]
    }

    override suspend fun getLatestBeforeDate(date: String): DailyInputEntity? {
        return map.filterKeys { it < date }
            .maxByOrNull { it.key }
            ?.value
    }
}

class DailyInputRepositoryTest {

    private lateinit var fakeDao: FakeDailyInputDao
    private lateinit var repository: DefaultDailyInputRepository

    @Before
    fun setUp() {
        fakeDao = FakeDailyInputDao()
        repository = DefaultDailyInputRepository(fakeDao)
    }

    @Test
    fun testSaveAndGetDailyInput() {
        runBlocking {
            val input = DailyInputEntity(
                date = "2026-10-20",
                weatherMorning = "nang",
                weatherAfternoon = "mua",
                weatherEvening = "binh_thuong",
                commandStaffCount = 4,
                workerCount = 35,
                equipmentList = "1 máy xúc, 2 xe lu, 1 cẩu tháp"
            )

            repository.saveDailyInput(input)

            val retrieved = repository.getDailyInputOrDefault("2026-10-20")
            assertEquals("nang", retrieved.weatherMorning)
            assertEquals("mua", retrieved.weatherAfternoon)
            assertEquals(4, retrieved.commandStaffCount)
            assertEquals(35, retrieved.workerCount)
            assertEquals("1 máy xúc, 2 xe lu, 1 cẩu tháp", retrieved.equipmentList)
        }
    }

    @Test
    fun testInheritFromPreviousDay() {
        runBlocking {
            // Ngày hôm qua
            val yesterdayInput = DailyInputEntity(
                date = "2026-10-19",
                weatherMorning = "nang",
                weatherAfternoon = "nang",
                weatherEvening = "binh_thuong",
                commandStaffCount = 5,
                workerCount = 42,
                equipmentList = "1 máy xúc Komatsu, 1 xe trộn bê tông"
            )
            repository.saveDailyInput(yesterdayInput)

            // Hôm nay chưa nhập gì
            val todayInput = repository.getDailyInputOrDefault("2026-10-20")

            // Đảm bảo kế thừa đúng nhân lực và máy móc hôm qua
            assertEquals("2026-10-20", todayInput.date)
            assertEquals(5, todayInput.commandStaffCount)
            assertEquals(42, todayInput.workerCount)
            assertEquals("1 máy xúc Komatsu, 1 xe trộn bê tông", todayInput.equipmentList)
            assertEquals("nang", todayInput.weatherMorning)
        }
    }
}
