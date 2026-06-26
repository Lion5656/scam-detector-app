package com.example.scamdetectorapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.scamdetectorapp.data.SettingsManager
import com.example.scamdetectorapp.service.MonitorService

class CallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallReceiver"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action: ${intent.action}")
        val settingsManager = SettingsManager(context)
        
        // 如果使用者關閉了「主動防護」開關，直接返回，不啟動背景監控服務
        if (!settingsManager.isProtectionEnabled) {
            Log.d(TAG, "Protection is disabled, ignoring broadcast.")
            return
        }

        if(intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED){
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d(TAG, "Phone State changed: $state")

            // 由於服務已在開啟防護時由 HomeScreen 預先啟動，
            // 這裡只需發送 Action 或直接再次呼叫 startService (系統會分發給已運行的服務)
            if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                Log.d(TAG, "Call detected, notifying MonitorService...")
                val serviceIntent = Intent(context, MonitorService::class.java)

                try {
                    context.startService(serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service from background: ${e.message}")
                    // 如果服務沒在跑，最後的嘗試才是啟動前景
                    context.startForegroundService(serviceIntent)
                }
            }
        }
    }
}
