package com.example.scamdetectorapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.scamdetectorapp.domain.model.DetectionMode

/**
 * 檢測紀錄 Entity
 */
@Entity(tableName = "detections")
data class DetectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // PHONE, URL, TEXT, PRICE
    val input: String,
    val riskLevel: String, // SAFE, SUSPICIOUS, DANGEROUS
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)
