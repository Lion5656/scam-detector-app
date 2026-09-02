package com.example.scamdetectorapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detection_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // 文字, URL, 電話, 圖片
    val riskLevel: String, // LOW, MEDIUM, HIGH, UNKNOWN, SAFE
    val content: String,
    val timestamp: Long,
    val score: Int,
    val category: String? = null
)
