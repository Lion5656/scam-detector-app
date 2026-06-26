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
import com.example.scamdetectorapp.ui.overlay.OverlayController
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    }

    // 要監控的特定 APP 的 Package Name
    private val targetAppPackages = listOf(
        "com.esunbank",
        "com.esunbank.oneyou",
        "jp.naver.line.android",
        "com.linecorp.line.android",
        "com.facebook.orca",
        "com.google.android.youtube",
        "com.linepaytw.upay",
        "tw.gov.post.mpost",
        "com.linebank.tw"
    )

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

    // 建立服務生命週期
    override fun onCreate(){
        Log.d(TAG, "service created")

        overlay = OverlayController(this)
        callDisconnectManager = CallDisconnectManager(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callStateMonitor = CallStateMonitor(this)
            callStateMonitor.startListening()

            stateListenJob = serviceScope.launch {
                // collectLatest 蒐集上游發射的值，新的值發射時，若上一次的操作未完成，會取消上一次的掛起操作
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
        } else {
            Log.w(TAG, "Device API level below S, call monitoring might not work as expected")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "service start command received")
        
        val settingsManager = SettingsManager(this)
        if (!settingsManager.isProtectionEnabled) {
            Log.d(TAG, "Protection disabled, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        return START_STICKY
    }

    // 終止生命週期，移除懸浮窗、停止執行緒
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
        
        // 僅檢查權限，若無權限則不啟動 monitorJob，由 HomeScreen 負責引導授權
        if(!hasUsageStatPermission()){
            Log.w(TAG, "缺少使用量存取權限，停止監控任務")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getForegroundAppPackageName()
        }

        if (monitorJob?.isActive == true) return

        monitorJob = serviceScope.launch {
            while(isActive){
                Log.d(TAG, "Monitoring...")

                val isSensitive = onCallStarted()
                if (isSensitive) {
                    if (!isCooldownActive()) {
                        changeState(MonitorState.Warning)
                        Log.d(TAG, "檢測到敏感APP")
                        showWarning()
                    } else {
                        Log.d(TAG, "cooldown active")
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
        // 如果現在距離上次警告時間「小於」冷卻時間，代表冷卻中
        return (System.currentTimeMillis() - lastWarningTime) < COOLDOWN_TIME
    }

    private suspend fun showWarning(){
        if(isOverlayShowing) return

        changeState(MonitorState.Warning)
        isOverlayShowing = true
        // 紀錄觸發警告的時間
        lastWarningTime = System.currentTimeMillis()
        
        // 強制返回主畫面
        backToHome()
        // 強制在主執行緒上執行
        withContext(Dispatchers.Main){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overlay.show(
                    onContinueClicked = {
                        enterCoolDown()
                        isOverlayShowing = false
                        Log.d(TAG, "點擊繼續使用")},
                    onEndCallClicked = {
                        callDisconnectManager.endCurrentCall()
                        isOverlayShowing = false
                        Log.d(TAG, "點擊掛斷電話")}
                )
            } else {
                Log.w(TAG, "Overlay not supported on this API level")
                isOverlayShowing = false
            }
        }
    }

    private fun enterCoolDown(){
        isOverlayShowing = false
        overlay.close()

        Log.d(TAG, "monitor cooldown")
        changeState(MonitorState.Cooldown)
    }

    private fun stopMonitoring(){
        isMonitoring = false
        // 關閉Job背景服務執行緒
        monitorJob?.cancel()
        // 重置警告時間為 0，讓下次電話開始時能立即觸發偵測
        lastWarningTime = 0L
        changeState(MonitorState.Idle)
        Log.d(TAG, "monitor stop")
    }

    private fun startForegroundServiceNotification(){
        val manager =  getSystemService(NotificationManager::class.java)

        val channelId = "monitor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 註冊管道
            val channel = NotificationChannel(channelId, TAG, IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        // 創建內容
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

        // 執行前景服務
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

    fun onCallStarted() : Boolean{
        // 啟動監控前景App
        val currentApp = getForegroundAppPackageName()

        if (currentApp != null && targetAppPackages.contains(currentApp)) {
            return true
        }
        return false
    }

    fun getForegroundAppPackageName() : String?{
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 30 * 1000 // 查詢過去 30 秒

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastResumedApp: String? = null

        while(usageEvents.hasNextEvent()){
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED){
                lastResumedApp = event.packageName
            }
        }

        if (lastResumedApp != null) {
            Log.d(TAG, "偵測到目前前景 APP: $lastResumedApp")
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
        
        Log.d(TAG, "UsageStats mode: $mode")
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
