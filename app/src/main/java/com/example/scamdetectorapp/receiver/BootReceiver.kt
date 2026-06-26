package com.example.scamdetectorapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.scamdetectorapp.data.SettingsManager
import com.example.scamdetectorapp.service.MonitorService

/**
 * 開機檢查主動防護狀態的接收器
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, checking protection status...")
            val settingsManager = SettingsManager(context)
            if (settingsManager.isProtectionEnabled) {
                Log.d("BootReceiver", "Protection enabled, starting MonitorService")
                val serviceIntent = Intent(context, MonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
