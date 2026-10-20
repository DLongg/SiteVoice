package com.sitevoice.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyInputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(input: DailyInputEntity)

    @Query("SELECT * FROM daily_inputs WHERE date = :date LIMIT 1")
    fun getByDate(date: String): Flow<DailyInputEntity?>

    @Query("SELECT * FROM daily_inputs WHERE date = :date LIMIT 1")
    suspend fun getByDateSync(date: String): DailyInputEntity?

    @Query("SELECT * FROM daily_inputs WHERE date < :date ORDER BY date DESC LIMIT 1")
    suspend fun getLatestBeforeDate(date: String): DailyInputEntity?
}
