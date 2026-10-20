package com.sitevoice.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_reports")
data class DailyReportEntity(
    @PrimaryKey
    val date: String, // yyyy-MM-dd
    val templateId: String = "nhat-ky-cong-truong",
    val contentJson: String, // JSON kết quả LLM theo schema
    val status: String = "DRAFT", // DRAFT, APPROVED, EXPORTED
    val docUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
