package com.example.scamdetectorapp.service

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Process
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.scamdetectorapp.manager.CallDisconnectManager
import com.example.scamdetectorapp.manager.CallStateMonitor
import com.example.scamdetectorapp.manager.ContactManager
import com.example.scamdetectorapp.ui.overlay.OverlayController
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.data.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

sealed interface MonitorState{
    data object Idle : MonitorState
    data object Monitoring : MonitorState
    data object Warning : MonitorState
    data object Cooldown : MonitorState
}

class MonitorService : Service(){
    companion object {
        private const val TAG = "MonitorService"
        private const val CHECK_INTERVAL = 3000L
        private const val COOLDOWN_TIME = 3 * 60 * 1000L
        
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }

    // 動態讀取受監控 APP 的 Package Name
    private var targetAppPackages = emptySet<String>()

    // 自定義白名單號碼
    private var customWhitelist = emptySet<String>()

    // 當前通話號碼
    private var currentCallNumber: String? = null

    // 聯絡人管理員
    private lateinit var contactManager: ContactManager
    private var isContactsEnabled: Boolean = false

    // 畫面狀態
    private var currentState : MonitorState = MonitorState.Idle

    // 背景相關協程
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var stateListenJob : Job? = null
    private var monitorJob : Job? = null

    private var isMonitoring : Boolean = true

    // 懸浮畫面
    private lateinit var overlay : OverlayController
    private var isOverlayShowing : Boolean = false

    // 冷卻時間相關
    private var lastWarningTime = 0L

    // 通話狀態監控器
    private lateinit var callStateMonitor: CallStateMonitor

    // 掛斷電話管理器
    private lateinit var callDisconnectManager: CallDisconnectManager

    private fun updateSettings() {
        val settingsManager = SettingsManager(this)
        serviceScope.launch {
            settingsManager.protectedApps.collect { apps ->
                targetAppPackages = apps
                Log.d(TAG, "Updated target apps: ${targetAppPackages.size}")
            }
        }
        serviceScope.launch {
            settingsManager.customWhitelist.collect { whitelist ->
                customWhitelist = whitelist
                Log.d(TAG, "Updated custom whitelist: ${customWhitelist.size}")
            }
        }
    }

    // 建立服務生命週期
    override fun onCreate(){
        Log.d(TAG, "service created")
        
        // 在 onCreate 最開始立即啟動前景通知
        startForegroundServiceNotification()
        
        updateSettings()
        
        contactManager = ContactManager(this)
        overlay = OverlayController(this)
        callDisconnectManager = CallDisconnectManager(this)

        // 監控設定開關
        val settingsManager = SettingsManager(this)
        serviceScope.launch {
            settingsManager.isProtectionEnabled.collect { enabled ->
                if (!enabled) {
                    Log.d(TAG, "Protection disabled from settings, stopping service")
                    stopSelf()
                }
            }
        }
        serviceScope.launch {
            settingsManager.isContactsEnabled.collect { enabled ->
                isContactsEnabled = enabled
                Log.d(TAG, "Contacts protection enabled: $isContactsEnabled")
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callStateMonitor = CallStateMonitor(this)
            callStateMonitor.startListening()

            stateListenJob = serviceScope.launch {
                callStateMonitor.callState.collectLatest { state ->
                    Log.d(TAG, "狀態 state $state")
                    if(state == TelephonyManager.CALL_STATE_OFFHOOK){
                        startMonitoring()
                    }
                    else{
                        stopMonitoring()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "service start command received")
        
        // 取得傳入的電話號碼
        val phoneNumber = intent?.getStringExtra(EXTRA_PHONE_NUMBER)
        if (phoneNumber != null) {
            currentCallNumber = phoneNumber
            Log.d(TAG, "Current call number updated: $currentCallNumber")
        }
        
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDestroy(){
        stopMonitoring()
        overlay.close()
        callStateMonitor.stopListening()
        serviceScope.cancel()
        Log.d(TAG, "service destroyed")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun startMonitoring(){
        changeState(MonitorState.Monitoring)
        if(!hasUsageStatPermission()){
            Log.w(TAG, "缺少使用量存取權限，停止監控任務")
            return
        }

        if (monitorJob?.isActive == true) return

        monitorJob = serviceScope.launch {
            while(isActive){
                Log.d(TAG, "Monitoring...")
                
                // 1. 檢查是否為信任聯絡人，如果是則不動作
                if (checkAndTrustContact()) {
                    delay(CHECK_INTERVAL)
                    continue
                }

                // 2. 檢查是否開啟敏感 App
                val isSensitive = onCallStarted()
                if (isSensitive) {
                    if (!isCooldownActive()) {
                        changeState(MonitorState.Warning)
                        showWarning()
                    }
                }
                delay(CHECK_INTERVAL)
            }
        }
    }

    private fun changeState(state : MonitorState){
        Log.d(TAG, "state changed: $currentState -> $state")
        currentState = state
    }

    private fun isCooldownActive(): Boolean {
        return (System.currentTimeMillis() - lastWarningTime) < COOLDOWN_TIME
    }

    private suspend fun showWarning(){
        if(isOverlayShowing) return
        changeState(MonitorState.Warning)
        isOverlayShowing = true
        lastWarningTime = System.currentTimeMillis()
        backToHome()
        withContext(Dispatchers.Main){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overlay.show(
                    onContinueClicked = {
                        enterCoolDown()
                        isOverlayShowing = false
                    },
                    onEndCallClicked = {
                        callDisconnectManager.endCurrentCall()
                        isOverlayShowing = false
                    }
                )
            }
        }
    }

    private fun enterCoolDown(){
        isOverlayShowing = false
        overlay.close()
        changeState(MonitorState.Cooldown)
    }

    private fun stopMonitoring(){
        isMonitoring = false
        monitorJob?.cancel()
        lastWarningTime = 0L
        changeState(MonitorState.Idle)
    }

    private fun startForegroundServiceNotification(){
        val manager =  getSystemService(NotificationManager::class.java)
        val channelId = "monitor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, TAG, IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.outline_shield_person_24)
            .setContentTitle("防詐監控啟動")
            .setContentText("安全保護中...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, builder.build())
        }
    }

    private fun backToHome(){
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun checkAndTrustContact(): Boolean {
        // 如果 Intent 沒帶號碼，嘗試從通話紀錄抓最後一筆
        if (currentCallNumber == null) {
            currentCallNumber = contactManager.getLastCallNumber()
            Log.d(TAG, "Attempted to recover number from CallLog: $currentCallNumber")
        }

        val number = currentCallNumber ?: return false
        
        // 1. 檢查自定義白名單 (需比對純數字)
        val rawNumber = number.filter { it.isDigit() }
        val isWhitelisted = customWhitelist.any { it.filter { digit -> digit.isDigit() } == rawNumber }
        
        if (isWhitelisted) {
            Log.d(TAG, "Call number $number is in custom whitelist, skipping monitoring")
            return true
        }

        // 2. 檢查通訊錄 (如果開啟)
        if (isContactsEnabled && contactManager.isNumberInContacts(number)) {
            Log.d(TAG, "Call number $number is in contacts, skipping monitoring")
            return true
        }
        
        return false
    }

    fun onCallStarted() : Boolean{
        // 啟動監控前景App
        val currentApp = getForegroundAppPackageName()
        return currentApp != null && targetAppPackages.contains(currentApp)
    }

    fun getForegroundAppPackageName() : String?{
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 30 * 1000
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastResumedApp: String? = null
        while(usageEvents.hasNextEvent()){
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED){
                lastResumedApp = event.packageName
            }
        }
        return lastResumedApp
    }

    fun hasUsageStatPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = @Suppress("DEPRECATION") appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
