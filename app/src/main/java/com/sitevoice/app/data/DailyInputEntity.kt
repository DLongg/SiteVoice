package com.sitevoice.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_inputs")
data class DailyInputEntity(
    @PrimaryKey
    val date: String, // yyyy-MM-dd
    val weatherMorning: String = "nang", // "nang" hoặc "mua"
    val weatherAfternoon: String = "nang", // "nang" hoặc "mua"
    val weatherEvening: String = "binh_thuong", // "binh_thuong" hoặc "mua"
    val commandStaffCount: Int = 0, // Ban chỉ huy công trường
    val workerCount: Int = 0, // Công nhân
    val equipmentList: String = "", // Máy móc - thiết bị thi công chính
    val updatedAt: Long = System.currentTimeMillis()
)
