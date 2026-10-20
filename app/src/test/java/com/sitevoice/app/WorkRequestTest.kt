package com.sitevoice.app

import androidx.work.NetworkType
import com.sitevoice.app.queue.ClassifyWorker
import com.sitevoice.app.queue.ExportWorker
import com.sitevoice.app.queue.SttWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkRequestTest {

    @Test
    fun testSttWorkRequestCreation() {
        val request = SttWorker.createWorkRequest(123L, "/path/to/audio.m4a")

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(123L, request.workSpec.input.getLong(SttWorker.KEY_NOTE_ID, -1L))
        assertEquals("/path/to/audio.m4a", request.workSpec.input.getString(SttWorker.KEY_AUDIO_PATH))
        assertTrue(request.tags.contains("SttWorker"))
        assertTrue(request.tags.contains("note_123"))
    }

    @Test
    fun testClassifyWorkRequestCreation() {
        val request = ClassifyWorker.createWorkRequest(456L, "Đổ bê tông sàn tầng 2")

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(456L, request.workSpec.input.getLong(ClassifyWorker.KEY_NOTE_ID, -1L))
        assertEquals("Đổ bê tông sàn tầng 2", request.workSpec.input.getString(ClassifyWorker.KEY_TEXT))
        assertTrue(request.tags.contains("ClassifyWorker"))
        assertTrue(request.tags.contains("note_456"))
    }

    @Test
    fun testExportWorkRequestCreation() {
        val request = ExportWorker.createWorkRequest("2026-10-20")

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals("2026-10-20", request.workSpec.input.getString(ExportWorker.KEY_DATE))
        assertTrue(request.tags.contains("ExportWorker"))
        assertTrue(request.tags.contains("export_2026-10-20"))
    }
}
