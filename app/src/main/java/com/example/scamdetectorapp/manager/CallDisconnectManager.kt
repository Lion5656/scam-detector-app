package com.example.scamdetectorapp.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallDisconnectManager(private val context : Context) {
    companion object {
        private const val TAG = "CallDisconnectManager"
    }
    fun endCurrentCall() : Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            // 檢查權限
            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
            if (hasPermission){
                // 呼叫telecomManager
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                
                // 執行掛斷
                try {
                    @Suppress("MissingPermission")
                    val success = telecomManager.endCall()
                    Log.d(TAG, "已送出掛斷指令，狀態: $success")
                    return success
                }
                catch(e: Exception) {
                    Log.e(TAG, "錯誤，無法掛斷來電: ${e.message}")
                    return false
                }
            }
            else{
                Log.e(TAG, "未取得 ANSWER_PHONE_CALLS 權限，無法執行")
                return false
            }
        } else {
            Log.e(TAG, "系統版本過低 (${Build.VERSION.SDK_INT} < ${Build.VERSION_CODES.P})，無法使用掛斷 API")
            return false
        }
    }

}