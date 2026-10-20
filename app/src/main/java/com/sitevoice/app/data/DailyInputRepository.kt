package com.sitevoice.app.data

import kotlinx.coroutines.flow.Flow

interface DailyInputRepository {
    fun getDailyInput(date: String): Flow<DailyInputEntity?>
    suspend fun getDailyInputOrDefault(date: String): DailyInputEntity
    suspend fun saveDailyInput(input: DailyInputEntity)
}

class DefaultDailyInputRepository(
    private val dao: DailyInputDao
) : DailyInputRepository {

    override fun getDailyInput(date: String): Flow<DailyInputEntity?> {
        return dao.getByDate(date)
    }

    override suspend fun getDailyInputOrDefault(date: String): DailyInputEntity {
        val existing = dao.getByDateSync(date)
        if (existing != null) {
            return existing
        }

        // Kế thừa từ ngày gần nhất trước đó nếu có
        val previousDay = dao.getLatestBeforeDate(date)
        return if (previousDay != null) {
            previousDay.copy(
                date = date,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            DailyInputEntity(date = date)
        }
    }

    override suspend fun saveDailyInput(input: DailyInputEntity) {
        dao.insertOrUpdate(input.copy(updatedAt = System.currentTimeMillis()))
    }
}
