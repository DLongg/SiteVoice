package com.sitevoice.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY isBuiltIn DESC, createdAt ASC")
    fun getAllTemplates(): Flow<List<TemplateItemEntity>>

    @Query("SELECT * FROM templates ORDER BY isBuiltIn DESC, createdAt ASC")
    suspend fun getAllTemplatesSync(): List<TemplateItemEntity>

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    fun getTemplateById(id: String): Flow<TemplateItemEntity?>

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateByIdSync(id: String): TemplateItemEntity?

    @Query("SELECT * FROM templates WHERE isActive = 1 LIMIT 1")
    fun getActiveTemplate(): Flow<TemplateItemEntity?>

    @Query("SELECT * FROM templates WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTemplateSync(): TemplateItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(template: TemplateItemEntity)

    @Update
    suspend fun update(template: TemplateItemEntity)

    @Query("DELETE FROM templates WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomTemplate(id: String): Int

    @Query("UPDATE templates SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE templates SET isActive = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun activateTemplate(id: String, updatedAt: Long = System.currentTimeMillis())

    @Transaction
    suspend fun setActiveTemplate(id: String) {
        deactivateAll()
        activateTemplate(id)
    }

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun countTemplates(): Int
}
