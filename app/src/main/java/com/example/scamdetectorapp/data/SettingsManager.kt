package com.example.scamdetectorapp.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("scam_detector_settings", Context.MODE_PRIVATE)

    var isProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROTECTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, value).apply()

    companion object {
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
    }
}
