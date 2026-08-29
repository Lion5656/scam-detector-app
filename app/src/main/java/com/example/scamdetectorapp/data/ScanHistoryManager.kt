package com.example.scamdetectorapp.data

import android.content.Context
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.domain.model.ScanRecord
import com.example.scamdetectorapp.presentation.model.ScanUiModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 負責管理掃描紀錄的儲存與讀取 (使用 SharedPreferences + Gson)
 */
class ScanHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("scan_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_HISTORY = "key_scan_history"

    fun saveRecord(record: ScanRecord) {
        val history = getHistory().toMutableList()
        // 檢查是否重複，若重複則先移除舊的
        history.removeAll { it.input == record.input && it.type == record.type }
        // 加入新紀錄到最前面
        history.add(0, record)
        // 最多保留 20 筆
        val limitedHistory = history.take(20)
        
        prefs.edit().putString(KEY_HISTORY, gson.toJson(limitedHistory)).apply()
    }

    fun getHistory(): List<ScanRecord> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ScanRecord>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
