package com.sitevoice.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(report: DailyReportEntity)

    @Query("SELECT * FROM daily_reports WHERE date = :date LIMIT 1")
    fun getByDate(date: String): Flow<DailyReportEntity?>

    @Query("SELECT * FROM daily_reports WHERE date = :date LIMIT 1")
    suspend fun getByDateSync(date: String): DailyReportEntity?

    @Query("UPDATE daily_reports SET status = :status, updatedAt = :updatedAt WHERE date = :date")
    suspend fun updateStatus(date: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE daily_reports SET docUrl = :docUrl, status = :status, updatedAt = :updatedAt WHERE date = :date")
    suspend fun updateDocUrl(date: String, docUrl: String, status: String = "EXPORTED", updatedAt: Long = System.currentTimeMillis())
}
