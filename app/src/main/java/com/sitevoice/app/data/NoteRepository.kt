package com.sitevoice.app.data

import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getActiveNotesByDate(date: String): Flow<List<NoteEntity>>
    fun getAllNotesByDate(date: String): Flow<List<NoteEntity>>
    suspend fun getNoteById(id: Long): NoteEntity?
    suspend fun insertNote(note: NoteEntity): Long
    suspend fun updateNote(note: NoteEntity)
    suspend fun updateText(id: Long, text: String)
    suspend fun updateLabel(id: Long, label: String, labelSource: String = "user")
    suspend fun softDelete(id: Long)
}

class DefaultNoteRepository(
    private val noteDao: NoteDao
) : NoteRepository {
    override fun getActiveNotesByDate(date: String): Flow<List<NoteEntity>> {
        return noteDao.getActiveNotesByDate(date)
    }

    override fun getAllNotesByDate(date: String): Flow<List<NoteEntity>> {
        return noteDao.getAllNotesByDate(date)
    }

    override suspend fun getNoteById(id: Long): NoteEntity? {
        return noteDao.getNoteById(id)
    }

    override suspend fun insertNote(note: NoteEntity): Long {
        return noteDao.insert(note)
    }

    override suspend fun updateNote(note: NoteEntity) {
        noteDao.update(note)
    }

    override suspend fun updateText(id: Long, text: String) {
        noteDao.updateText(id, text)
    }

    override suspend fun updateLabel(id: Long, label: String, labelSource: String) {
        noteDao.updateLabel(id, label, labelSource)
    }

    override suspend fun softDelete(id: Long) {
        noteDao.softDelete(id)
    }
}
