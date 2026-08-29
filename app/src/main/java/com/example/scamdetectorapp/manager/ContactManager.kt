package com.example.scamdetectorapp.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

class ContactManager(private val context: Context) {
    companion object {
        private const val TAG = "ContactManager"
    }

    /**
     * 檢查特定號碼是否在聯絡人名單中
     */
    fun isNumberInContacts(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        
        // 檢查聯絡人權限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing READ_CONTACTS permission")
            return false
        }

        // 正規化號碼：移除所有非數字與非 + 號字元
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val found = cursor.moveToFirst()
                if (found) {
                    val name = cursor.getString(0)
                    Log.d(TAG, "Found contact for $phoneNumber: $name")
                }
                found
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking contacts: ${e.message}")
            false
        }
    }

    /**
     * 從通話紀錄中取得最後一筆通話號碼
     */
    fun getLastCallNumber(): String? {
        // 檢查通話紀錄權限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing READ_CALL_LOG permission")
            return null
        }

        val projection = arrayOf(CallLog.Calls.NUMBER)
        
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // 使用 Bundle 方式限制數量 (API 26+)
                val queryArgs = android.os.Bundle().apply {
                    putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${CallLog.Calls.DATE} DESC")
                    putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, 1)
                }
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    queryArgs,
                    null
                )
            } else {
                // 舊版相容方式
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )
            }?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val number = cursor.getString(0)
                    Log.d(TAG, "Latest call number from log: $number")
                    number
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last call number: ${e.message}")
            null
        }
    }
}
