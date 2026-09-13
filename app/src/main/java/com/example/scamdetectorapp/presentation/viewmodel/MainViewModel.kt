package com.example.scamdetectorapp.presentation.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.scamdetectorapp.data.SettingsManager
import com.example.scamdetectorapp.data.repository.AntiFraudRepository
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.domain.model.ScanResult
import com.example.scamdetectorapp.presentation.model.ScanUiModel
import com.example.scamdetectorapp.presentation.model.DashboardStats
import com.example.scamdetectorapp.presentation.model.ScamTypeRatio
import com.example.scamdetectorapp.data.local.entity.HistoryEntity
import com.example.scamdetectorapp.BuildConfig
import androidx.compose.ui.graphics.Color
import com.example.scamdetectorapp.manager.PermissionManager
import com.example.scamdetectorapp.manager.PermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Loading : ScanUiState
    data class Success(val result: ScanUiModel) : ScanUiState
    data class Error(val message: String, val title: String = "錯誤") : ScanUiState
}


class MainViewModel(application: Application, private val repository: AntiFraudRepository) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)
    private val permissionManager = PermissionManager(application)

    // 從 DataStore Flow 轉換為 StateFlow 以供 Compose 使用
    val isProtectionEnabled = settingsManager.isProtectionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val isContactsEnabled = settingsManager.isContactsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isShareAutoInputEnabled = settingsManager.isShareAutoInputEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val protectedApps = settingsManager.protectedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val customWhitelist = settingsManager.customWhitelist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // 權限相關狀態
    val permissionStatus = permissionManager.permissionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PermissionStatus())

    private val _highlightPermissionCenter = MutableStateFlow(false)
    val highlightPermissionCenter = _highlightPermissionCenter.asStateFlow()

    // 儲存各模式的【狀態】內容，避免切換分頁時遺失
    private val _urlState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    private val _phoneState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    private val _textState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    private val _priceState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)

    // 儲存各模式的【輸入】內容，避免切換分頁時遺失
    private val _urlInput = MutableStateFlow("")
    private val _phoneInput = MutableStateFlow("")
    private val _textInput = MutableStateFlow("")
    private val _priceInput = MutableStateFlow("")

    // 將 asStateFlow() 的結果快取起來，避免重複建立物件
    val urlState = _urlState.asStateFlow()
    val phoneState = _phoneState.asStateFlow()
    val textState = _textState.asStateFlow()
    val priceState = _priceState.asStateFlow()

    // 將 asStateFlow() 的結果快取起來，避免重複建立物件
    val urlInput = _urlInput.asStateFlow()
    val phoneInput = _phoneInput.asStateFlow()
    val textInput = _textInput.asStateFlow()
    val priceInput = _priceInput.asStateFlow()

    init {
        updatePermissionStatus()
        syncShareComponentState()
        observePermissionAndProtection()
    }

    private fun observePermissionAndProtection() {
        viewModelScope.launch {
            // 等待首次權限檢查完成，避免啟動時因為初始值為 false 而誤關閉防護
            permissionManager.updatePermissionStatus()

            permissionStatus.collect { _ ->
                // 檢查核心權限
                if (!hasDisplayPermissions() && isProtectionEnabled.value) {
                    Log.d("MainViewModel", "Core permissions lost, disabling protection")
                    settingsManager.setProtectionEnabled(false)
                }
            }
        }
    }
    // --- 新聞資料狀態 ---
    private val _latestNews = MutableStateFlow<List<com.example.scamdetectorapp.data.repository.NewsItem>>(emptyList())
    val latestNews = _latestNews.asStateFlow()

    fun refreshNews() {
        viewModelScope.launch {
            _latestNews.value = com.example.scamdetectorapp.data.repository.NewsRepository.fetchAllLatestNews()
        }
    }

    val allHistory: StateFlow<List<HistoryEntity>> = repository.getAllHistory()
        ?.map { history ->
            if (BuildConfig.DEBUG) {
                val mockHistory = mutableListOf<HistoryEntity>()
                
                // 動態取得本週一的凌晨時間
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val currentDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                val daysToMinus = if (currentDayOfWeek == java.util.Calendar.SUNDAY) 6 else (currentDayOfWeek - java.util.Calendar.MONDAY)
                cal.add(java.util.Calendar.DAY_OF_YEAR, -daysToMinus)
                val mondayTime = cal.timeInMillis

                // 定義一週的數據比例 (低, 中, 高)
                val weeklyRatios = listOf(
                    Triple(2, 1, 0), // Mon - 3筆 (黃)
                    Triple(1, 0, 0), // Tue - 1筆 (綠)
                    Triple(3, 2, 5), // Wed - 10筆 (紅)
                    Triple(2, 4, 9), // Thu - 15筆 (紅)
                    Triple(6, 1, 3), // Fri - 10筆 (紅)
                    Triple(2, 2, 0), // Sat - 4筆 (紅/黃)
                    Triple(4, 2, 4)  // Sun - 10筆 (紅)
                )

                weeklyRatios.forEachIndexed { dayIdx, risks ->
                    val dayBase = mondayTime + dayIdx * 24 * 60 * 60 * 1000L
                    
                    // 注入低風險
                    repeat(risks.first) { i ->
                        mockHistory.add(HistoryEntity(0, "文字", "SAFE", "測試數據", dayBase + i * 1000, 20, "正常訊息"))
                    }
                    // 注入中風險
                    repeat(risks.second) { i ->
                        mockHistory.add(HistoryEntity(0, "電話", "MEDIUM", "測試數據", dayBase + (risks.first + i) * 1000, 55, "疑似廣告"))
                    }
                    // 注入高風險
                    repeat(risks.third) { i ->
                        val category = if (i % 2 == 0) "假投資" else "約會交友"
                        mockHistory.add(HistoryEntity(0, "簡訊", "HIGH", "測試數據", dayBase + (risks.first + risks.second + i) * 1000, 85, category))
                    }
                }
                history + mockHistory
            } else {
                history
            }
        }
        ?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        ?: MutableStateFlow(emptyList())

    // 儀表板統計數據
    val dashboardStats: StateFlow<DashboardStats> = allHistory.map { history ->
        val highRiskCount = history.count { it.riskLevel == "HIGH" }
        
        // 1. 詐騙類型分佈比例 (按媒介區分)
        val defaultMediaCategories = listOf("文字", "簡訊", "電話", "圖片")
        val mediaColors = mapOf(
            "文字" to Color(0xFFF2C94C), // 琥珀黃
            "簡訊" to Color(0xFFF05A5A), // 珊瑚紅
            "電話" to Color(0xFF4F7CFF), // 科技藍
            "圖片" to Color(0xFF00C853)  // 翡翠綠
        )

        val highRiskHistory = history.filter { it.riskLevel == "HIGH" }
        val highRiskTotal = highRiskHistory.size
        
        val mediaDistribution = defaultMediaCategories.map { category ->
            val count = highRiskHistory.count { it.type == category || (category == "簡訊" && it.type == "URL") }
            ScamTypeRatio(
                label = category,
                percentage = if (highRiskTotal > 0) (count * 100 / highRiskTotal) else 0,
                color = mediaColors[category] ?: Color.Gray
            )
        }

        // 2. 詐騙電話種類統計 (按詳細行為區分)
        // 篩選出所有高風險的電話紀錄 (或根據您的需求包含簡訊)
        val detailedScams = highRiskHistory.filter { it.type == "電話" || it.type == "簡訊" }
        val detailedTotal = detailedScams.size
        
        val defaultDetailCategories = listOf("約會交友", "假包裹釣魚", "假投資", "假求職", "假信貸", "假冒公務", "假冒電商", "商業騷擾", "其他")
        val detailColors = listOf(
            Color(0xFFF05A5A), Color(0xFFFFA905), Color(0xFFF2C94C), 
            Color(0xFF00C853), Color(0xFF4F7CFF), Color(0xFF4B0082), 
            Color(0xFFA78BFA), Color(0xFF64748B), Color(0xFFB0BEC5)
        )
        
        val phoneTypeDistribution = defaultDetailCategories.mapIndexed { idx, category ->
            val count = detailedScams.count { 
                if (category == "其他") it.category !in defaultDetailCategories
                else it.category == category
            }
            ScamTypeRatio(
                label = category,
                percentage = if (detailedTotal > 0) (count * 100 / detailedTotal) else 0,
                color = detailColors.getOrElse(idx) { Color.Gray }
            )
        }

        // 計算本週趨勢 (週一至週日)
        val trendLabels = mutableListOf<String>()
        val lowRiskTrend = mutableListOf<Float>()
        val mediumRiskTrend = mutableListOf<Float>()
        val highRiskTrend = mutableListOf<Float>()

        val dayFormatter = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())

        val calendar = java.util.Calendar.getInstance()
        // 設定到今天凌晨
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        // 在 Java Calendar 中，週日是 1，週一是 2
        // 我們要推算回本週一
        val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        // 如果今天是週日 (1)，則減 6 天得到週一；否則減 (目前-2) 天
        val daysToMinus = if (currentDayOfWeek == java.util.Calendar.SUNDAY) 6 else (currentDayOfWeek - java.util.Calendar.MONDAY)
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysToMinus)

        for (i in 0..6) {
            val dayStart = calendar.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
            
            val dayHistory = history.filter { it.timestamp in dayStart..dayEnd }
            
            trendLabels.add(dayFormatter.format(calendar.time))
            lowRiskTrend.add(dayHistory.count { it.riskLevel == "SAFE" || it.riskLevel == "LOW" }.toFloat())
            mediumRiskTrend.add(dayHistory.count { it.riskLevel == "MEDIUM" }.toFloat())
            highRiskTrend.add(dayHistory.count { it.riskLevel == "HIGH" }.toFloat())
            
            // 往後推一天
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        DashboardStats(
            highRiskMessages = highRiskCount,
            interceptedCount = highRiskCount,
            learningProgress = (history.size * 5).coerceAtMost(100),
            reportedCases = highRiskCount,
            typeDistribution = mediaDistribution,
            phoneTypeDistribution = phoneTypeDistribution,
            trendData = com.example.scamdetectorapp.presentation.model.RiskTrendData(
                lowRisk = lowRiskTrend,
                mediumRisk = mediumRiskTrend,
                highRisk = highRiskTrend,
                labels = trendLabels
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats(0, 0, 0, 0, emptyList(), emptyList(), com.example.scamdetectorapp.presentation.model.RiskTrendData(emptyList(), emptyList(), emptyList(), emptyList())))

    fun getState(mode: DetectionMode): StateFlow<ScanUiState> = when (mode) {
        DetectionMode.URL -> urlState
        DetectionMode.PHONE -> phoneState
        DetectionMode.TEXT -> textState
        DetectionMode.PRICE -> priceState
    }

    private fun getMutableState(mode: DetectionMode): MutableStateFlow<ScanUiState> = when (mode) {
        DetectionMode.URL -> _urlState
        DetectionMode.PHONE -> _phoneState
        DetectionMode.TEXT -> _textState
        DetectionMode.PRICE -> _priceState
    }

    fun getInput(mode: DetectionMode): StateFlow<String> = when (mode) {
        DetectionMode.URL -> urlInput
        DetectionMode.PHONE -> phoneInput
        DetectionMode.TEXT -> textInput
        DetectionMode.PRICE -> priceInput
    }

    fun setInput(mode: DetectionMode, text: String) {
        when (mode) {
            DetectionMode.URL -> _urlInput.value = text
            DetectionMode.PHONE -> _phoneInput.value = text
            DetectionMode.TEXT -> _textInput.value = text
            DetectionMode.PRICE -> _priceInput.value = text
        }
    }

    fun resetState(mode: DetectionMode) {
        getMutableState(mode).value = ScanUiState.Idle
    }

    fun resetAllStates() {
        listOf(DetectionMode.URL, DetectionMode.PHONE, DetectionMode.TEXT, DetectionMode.PRICE).forEach { mode ->
            getMutableState(mode).value = ScanUiState.Idle
            setInput(mode, "")
        }
    }

    private fun syncShareComponentState() {
        viewModelScope.launch {
            val isEnabled = settingsManager.isShareAutoInputEnabled.first()
            updateShareComponent(isEnabled)
        }
    }

    private fun updateShareComponent(enabled: Boolean) {
        try {
            val context = getApplication<Application>()
            val componentName = ComponentName(context, "${context.packageName}.ShareEntryActivity")
            context.packageManager.setComponentEnabledSetting(
                componentName,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("MainViewModel", "Share component enabled: $enabled")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to update share component", e)
        }
    }


    fun updatePermissionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            permissionManager.updatePermissionStatus()
        }
    }

    fun hasDisplayPermissions(): Boolean {
        return permissionManager.hasDisplayPermissions()
    }

    fun hasContactsPermission(): Boolean {
        return permissionManager.hasContactsPermission()
    }

    fun hasCallLogPermission(): Boolean {
        return permissionManager.hasCallLogsPermission()
    }

    fun setHighlightPermission(highlight: Boolean) {
        _highlightPermissionCenter.value = highlight
    }

    fun toggleProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setProtectionEnabled(enabled)
        }
    }

    fun toggleContactsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setContactsEnabled(enabled)
        }
    }

    fun toggleShareAutoInputEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setShareAutoInputEnabled(enabled)
            updateShareComponent(enabled)
        }
    }

    fun updateProtectedApps(apps: Set<String>) {
        viewModelScope.launch {
            settingsManager.updateProtectedApps(apps)
        }
    }

    fun addWhitelistNumber(number: String) {
        viewModelScope.launch {
            val current = customWhitelist.value.toMutableSet()
            if (current.add(number)) {
                settingsManager.updateCustomWhitelist(current)
            }
        }
    }

    fun removeWhitelistNumber(number: String) {
        viewModelScope.launch {
            val current = customWhitelist.value.toMutableSet()
            if (current.remove(number)) {
                settingsManager.updateCustomWhitelist(current)
            }
        }
    }

    fun scan(mode: DetectionMode, input: String) {
        val stateFlow = getMutableState(mode)
        stateFlow.value = ScanUiState.Loading
        
        viewModelScope.launch {
            val result = repository.scan(mode, input.trim())
            result.fold(
                onSuccess = { scanResult ->
                    val uiModel = mapToUiModel(scanResult, mode)
                    stateFlow.value = ScanUiState.Success(uiModel)
                    saveToHistory(mode, input, uiModel)
                    if (mode == DetectionMode.PRICE) {
                        Toast.makeText(getApplication(), "商品分析傳送成功", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { e ->
                    val (title, message) = when (e) {
                        is HttpException -> "伺服器錯誤 (${e.code()})" to (e.response()?.errorBody()?.string() ?: "無詳細錯誤訊息")
                        is com.google.gson.JsonSyntaxException -> "資料格式錯誤" to "API 回傳了非 JSON 格式的資料"
                        is SocketTimeoutException -> "連線逾時" to "伺服器回應太慢，請稍後再試"
                        else -> "錯誤" to (e.message ?: "發生未知錯誤")
                    }
                    stateFlow.value = ScanUiState.Error(message, title)
                }
            )
        }
    }

    private fun saveToHistory(mode: DetectionMode, input: String, uiModel: ScanUiModel) {
        viewModelScope.launch {
            val type = when (mode) {
                DetectionMode.URL -> "簡訊"  // 統一存為簡訊
                DetectionMode.PHONE -> "電話"
                DetectionMode.TEXT -> "文字"
                DetectionMode.PRICE -> "圖片"
            }
            val content = if (mode == DetectionMode.PRICE) {
                input.removePrefix("uri:").substringAfterLast("/")
            } else input

            val history = HistoryEntity(
                type = type,
                riskLevel = uiModel.riskLevel,
                content = content,
                timestamp = System.currentTimeMillis(),
                score = uiModel.score,
                category = uiModel.detailMap?.get("電話類型")?.toString()
            )
            repository.saveHistory(history)
        }
    }

    private fun mapToUiModel(result: ScanResult, mode: DetectionMode): ScanUiModel {
        val riskLevel = result.riskLevel?.trim() ?: ""
        val reasons = mutableListOf<String>()
        val title: String

        val rLevel = when {
            riskLevel.contains("高") -> "HIGH"
            riskLevel.contains("中")  -> "MEDIUM"
            riskLevel.contains("低") -> "LOW"
            riskLevel.contains("未知") -> "UNKNOWN"
            else -> riskLevel.uppercase()
        }

        var score = result.score?.toIntOrNull() ?: when (rLevel.uppercase()) {
            "HIGH" -> 85
            "MEDIUM" -> 60
            "LOW" -> 20
            "SAFE" -> 10
            "NODATA" -> 0
            else -> 0
        }

        if (mode == DetectionMode.PHONE) {
            val reports = result.detailInfo?.get("回報次數")?.toString()?.toIntOrNull() ?: 0
            if (reports > 0) {
                score = (score + (reports * 5)).coerceAtMost(100)
            }
        }

        when (rLevel.uppercase()) {
            "HIGH", "MEDIUM", "LOW" -> {
                title = when (rLevel.uppercase()) {
                    "HIGH" -> "高風險威脅"
                    "MEDIUM" -> "中風險威脅"
                    else -> "低風險威脅"
                }
                reasons.add("風險等級: $rLevel")
                result.threatType?.takeIf { it.isNotEmpty() }?.let { reasons.add("類型: $it") }
                result.suggestion?.takeIf { it.isNotEmpty() }?.let { reasons.add(it) }
            }
            "SAFE" -> {
                title = "安全內容"
                if (mode == DetectionMode.PHONE) {
                    reasons.add("此號碼目前暫無回報紀錄")
                } else {
                    reasons.add("無詐騙特徵")
                    reasons.add("正規網域/號碼/內容")
                }
            }
            else -> {
                title = "未知"
                reasons.add("暫無此紀錄")
            }
        }

        return ScanUiModel(
            isSafe = (rLevel == "SAFE" || rLevel == "NODATA"),
            riskLevel = rLevel,
            score = score,
            title = title,
            reasons = reasons,
            mode = mode,
            detailMap = result.detailInfo
        )
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(application, AntiFraudRepository(application)) as T
            }
        }

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                throw IllegalStateException("MainViewModel requires Application. Use provideFactory(application) instead.")
            }
        }
    }
}
