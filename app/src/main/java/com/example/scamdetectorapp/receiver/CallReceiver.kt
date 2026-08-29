package com.example.scamdetectorapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.scamdetectorapp.data.SettingsManager
import com.example.scamdetectorapp.manager.ContactManager
import com.example.scamdetectorapp.service.MonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallReceiver"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val pendingResult = goAsync()
            val settingsManager = SettingsManager(context)
            val contactManager = ContactManager(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (!settingsManager.isProtectionEnabled.first()) {
                        return@launch
                    }

                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                        // 嘗試從 Intent 取得號碼
                        var phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                        
                        // 如果是撥出電話或系統未提供 Extra，延遲一下查詢通話紀錄 (讓系統有時間寫入)
                        if (phoneNumber == null) {
                            delay(1000)
                            phoneNumber = contactManager.getLastCallNumber()
                        }

                        Log.d(TAG, "Call detected. Captured number: $phoneNumber")
                        
                        val serviceIntent = Intent(context, MonitorService::class.java).apply {
                            if (phoneNumber != null) {
                                putExtra(MonitorService.EXTRA_PHONE_NUMBER, phoneNumber)
                            }
                        }
                        
                        try {
                            context.startService(serviceIntent)
                        } catch (e: Exception) {
                            context.startForegroundService(serviceIntent)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
