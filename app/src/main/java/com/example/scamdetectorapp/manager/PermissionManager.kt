package com.example.scamdetectorapp.manager

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// 權限狀態模型
data class PermissionStatus(
    val hasOverlay: Boolean = false,
    val hasUsageStats: Boolean = false,
    val hasPhoneState: Boolean = false,
    val hasContacts: Boolean = false,
    val hasCallLogs: Boolean = false,
)

class PermissionManager(private val context: Context) {
    private var _permissionStatus = MutableStateFlow(PermissionStatus())
    val permissionStatus = _permissionStatus.asStateFlow()

    fun hasDisplayPermissions() : Boolean{
        val status = _permissionStatus.value
        return status.hasOverlay && status.hasUsageStats && status.hasPhoneState
    }

    fun hasContactsPermission() : Boolean {
        val status = _permissionStatus.value
        return status.hasContacts
    }

    fun hasCallLogsPermission() : Boolean {
        val status = _permissionStatus.value
        return status.hasCallLogs
    }

    fun updatePermissionStatus() : PermissionStatus{
        val status = PermissionStatus(
            hasOverlay = Settings.canDrawOverlays(context),
            hasUsageStats = hasUsageStatPermission(context),
            hasPhoneState = isGranted(Manifest.permission.READ_PHONE_STATE),
            hasContacts = isGranted(Manifest.permission.READ_CONTACTS),
            hasCallLogs = isGranted(Manifest.permission.READ_CALL_LOG)
        )
        _permissionStatus.value = status
        return status
    }

    private fun isGranted(permission : String) : Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasUsageStatPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}