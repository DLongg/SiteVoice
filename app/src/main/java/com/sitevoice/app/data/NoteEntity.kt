package com.sitevoice.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    val date: String, // yyyy-MM-dd
    val audioPath: String,
    val durationMs: Long,
    val text: String? = null,
    val sttSource: String? = null,
    val label: String? = null,
    val labelSource: String? = null,
    val status: String, // RECORDED, TRANSCRIBED, LABELED, DISCARDED
    val error: String? = null
)
