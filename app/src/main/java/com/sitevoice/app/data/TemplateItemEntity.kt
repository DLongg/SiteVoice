package com.sitevoice.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val docTemplateUrl: String = "",
    val systemPrompt: String,
    val schemaJson: String,
    val isBuiltIn: Boolean = false,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
