package com.example.scamdetectorapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDao {
    /**
     * 取得最新 5 筆紀錄 (Room DAO 查詢語法)
     */
    @Query("SELECT * FROM detections ORDER BY timestamp DESC LIMIT 5")
    fun getRecentScans(): Flow<List<DetectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(entity: DetectionEntity)

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<DetectionEntity>>
}
