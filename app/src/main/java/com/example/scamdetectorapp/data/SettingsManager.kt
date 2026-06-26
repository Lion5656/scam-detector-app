package com.example.scamdetectorapp.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 應用程式設定管理員
 * 負責持久化儲存使用者的偏好設定，如「主動防護」開關狀態。
 */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("scam_detector_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
    }

    /**
     * 主動防護是否開啟
     */
    var isProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROTECTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, value).apply()
}
