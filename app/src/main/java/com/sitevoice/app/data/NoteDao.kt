package com.sitevoice.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE date = :date AND status != 'DISCARDED' ORDER BY createdAt DESC")
    fun getActiveNotesByDate(date: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE date = :date ORDER BY createdAt DESC")
    fun getAllNotesByDate(date: String): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET status = 'DISCARDED' WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE notes SET text = :text WHERE id = :id")
    suspend fun updateText(id: Long, text: String)

    @Query("UPDATE notes SET label = :label, labelSource = :labelSource, status = 'LABELED' WHERE id = :id")
    suspend fun updateLabel(id: Long, label: String, labelSource: String)
}
