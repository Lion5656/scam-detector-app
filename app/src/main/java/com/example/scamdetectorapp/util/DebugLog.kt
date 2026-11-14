package com.example.scamdetectorapp.util

import android.util.Log
import com.example.scamdetectorapp.BuildConfig

/**
 * 僅於 DEBUG 版輸出的除錯 Log。
 * 除錯訊息常含電話號碼、聯絡人名稱或請求網址，release 版一律不輸出，避免個資外洩至 Logcat。
 * 警告與錯誤請直接使用 Log.w / Log.e。
 */
object DebugLog {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }
}
