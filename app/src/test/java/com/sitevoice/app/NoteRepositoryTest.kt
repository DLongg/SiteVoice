package com.sitevoice.app

import com.sitevoice.app.data.DefaultNoteRepository
import com.sitevoice.app.data.NoteDao
import com.sitevoice.app.data.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeNoteDao : NoteDao {
    private val notes = mutableListOf<NoteEntity>()
    private val notesFlow = MutableStateFlow<List<NoteEntity>>(emptyList())
    private var nextId = 1L

    private fun refreshFlow() {
        notesFlow.value = notes.toList()
    }

    override suspend fun insert(note: NoteEntity): Long {
        val id = if (note.id == 0L) nextId++ else note.id
        val entity = note.copy(id = id)
        notes.removeAll { it.id == id }
        notes.add(entity)
        refreshFlow()
        return id
    }

    override suspend fun update(note: NoteEntity) {
        notes.indexOfFirst { it.id == note.id }.takeIf { it != -1 }?.let { index ->
            notes[index] = note
            refreshFlow()
        }
    }

    override suspend fun delete(note: NoteEntity) {
        notes.removeAll { it.id == note.id }
        refreshFlow()
    }

    override suspend fun getNoteById(id: Long): NoteEntity? {
        return notes.find { it.id == id }
    }

    override fun getActiveNotesByDate(date: String): Flow<List<NoteEntity>> {
        return notesFlow.map { list ->
            list.filter { it.date == date && it.status != "DISCARDED" }
                .sortedByDescending { it.createdAt }
        }
    }

    override fun getAllNotesByDate(date: String): Flow<List<NoteEntity>> {
        return notesFlow.map { list ->
            list.filter { it.date == date }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun softDelete(id: Long) {
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(status = "DISCARDED")
            refreshFlow()
        }
    }

    override suspend fun updateText(id: Long, text: String) {
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(text = text)
            refreshFlow()
        }
    }

    override suspend fun updateLabel(id: Long, label: String, labelSource: String) {
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(label = label, labelSource = labelSource, status = "LABELED")
            refreshFlow()
        }
    }
}

class NoteRepositoryTest {

    private lateinit var fakeDao: FakeNoteDao
    private lateinit var repository: DefaultNoteRepository

    @Before
    fun setUp() {
        fakeDao = FakeNoteDao()
        repository = DefaultNoteRepository(fakeDao)
    }

    @Test
    fun testInsertAndGetById() {
        runBlocking {
            val note = NoteEntity(
                createdAt = System.currentTimeMillis(),
                date = "2026-10-20",
                audioPath = "/storage/audio/note1.m4a",
                durationMs = 5000,
                text = "Đổ bê tông sàn tầng 2",
                status = "TRANSCRIBED"
            )

            val id = repository.insertNote(note)
            assertTrue("ID should be > 0", id > 0)

            val fetched = repository.getNoteById(id)
            assertNotNull("Note should exist", fetched)
            assertEquals("Đổ bê tông sàn tầng 2", fetched?.text)
            assertEquals("TRANSCRIBED", fetched?.status)
        }
    }

    @Test
    fun testGetActiveNotesFiltersDiscarded() {
        runBlocking {
            val note1 = NoteEntity(
                createdAt = 1000L,
                date = "2026-10-20",
                audioPath = "/audio/1.m4a",
                durationMs = 2000,
                text = "Ghi chú 1",
                status = "TRANSCRIBED"
            )
            val note2 = NoteEntity(
                createdAt = 2000L,
                date = "2026-10-20",
                audioPath = "/audio/2.m4a",
                durationMs = 3000,
                text = "Ghi chú 2 bị hủy",
                status = "DISCARDED"
            )

            val id1 = repository.insertNote(note1)
            val id2 = repository.insertNote(note2)

            val activeNotes = repository.getActiveNotesByDate("2026-10-20").first()
            assertEquals("Should only have 1 active note", 1, activeNotes.size)
            assertEquals(id1, activeNotes[0].id)

            val allNotes = repository.getAllNotesByDate("2026-10-20").first()
            assertEquals("All notes should include discarded", 2, allNotes.size)
        }
    }

    @Test
    fun testSoftDelete() {
        runBlocking {
            val note = NoteEntity(
                createdAt = 1000L,
                date = "2026-10-20",
                audioPath = "/audio/1.m4a",
                durationMs = 2000,
                text = "Ghi chú sẽ xóa",
                status = "TRANSCRIBED"
            )

            val id = repository.insertNote(note)
            repository.softDelete(id)

            val activeNotes = repository.getActiveNotesByDate("2026-10-20").first()
            assertTrue("Active notes should be empty after soft delete", activeNotes.isEmpty())

            val fetched = repository.getNoteById(id)
            assertEquals("DISCARDED", fetched?.status)
            assertEquals("/audio/1.m4a", fetched?.audioPath) // File path vẫn được giữ
        }
    }

    @Test
    fun testUpdateText() {
        runBlocking {
            val note = NoteEntity(
                createdAt = 1000L,
                date = "2026-10-20",
                audioPath = "/audio/1.m4a",
                durationMs = 2000,
                text = "Văn bản ban đầu",
                status = "TRANSCRIBED"
            )

            val id = repository.insertNote(note)
            repository.updateText(id, "Văn bản đã được chỉnh sửa bằng tay")

            val updated = repository.getNoteById(id)
            assertEquals("Văn bản đã được chỉnh sửa bằng tay", updated?.text)
        }
    }

    @Test
    fun testUpdateLabel() {
        runBlocking {
            val note = NoteEntity(
                createdAt = 1000L,
                date = "2026-10-20",
                audioPath = "/audio/1.m4a",
                durationMs = 2000,
                text = "Kiểm tra cốt thép",
                status = "TRANSCRIBED"
            )

            val id = repository.insertNote(note)
            repository.updateLabel(id, "thi_cong", "user")

            val updated = repository.getNoteById(id)
            assertEquals("thi_cong", updated?.label)
            assertEquals("user", updated?.labelSource)
            assertEquals("LABELED", updated?.status)
        }
    }
}
