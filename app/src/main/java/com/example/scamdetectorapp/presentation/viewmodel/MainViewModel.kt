package com.example.scamdetectorapp.presentation.viewmodel

import com.example.scamdetectorapp.util.DebugLog
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
import com.example.scamdetectorapp.presentation.model.PhoneGenealogyData
import com.example.scamdetectorapp.presentation.model.ScanUiModel
import com.example.scamdetectorapp.presentation.model.DashboardStats
import com.example.scamdetectorapp.presentation.model.ScamTypeRatio
import com.example.scamdetectorapp.data.local.entity.HistoryEntity
import com.example.scamdetectorapp.data.local.entity.PhoneHistoryEntity
import androidx.compose.ui.graphics.Color
import com.example.scamdetectorapp.manager.PermissionManager
import com.example.scamdetectorapp.manager.PermissionStatus
import android.content.Intent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.scamdetectorapp.presentation.model.InstalledAppInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Loading : ScanUiState
    data class Success(val result: ScanUiModel) : ScanUiState
    data class Error(val message: String, val title: String = "錯誤") : ScanUiState
}

// 號碼族譜頁面的載入狀態
sealed interface PhoneGenealogyUiState {
    object Idle : PhoneGenealogyUiState
    object Loading : PhoneGenealogyUiState
    data class Success(val data: PhoneGenealogyData) : PhoneGenealogyUiState
    data class Error(val message: String, val title: String = "錯誤") : PhoneGenealogyUiState
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

    // Eagerly：App 啟動時即讀取，進入設定頁時防護數量不會先閃現 0
    val protectedApps = settingsManager.protectedApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val customWhitelist = settingsManager.customWhitelist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // 權限相關狀態
    val permissionStatus = permissionManager.permissionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PermissionStatus())

    private val _highlightPermissionCenter = MutableStateFlow(false)
    val highlightPermissionCenter = _highlightPermissionCenter.asStateFlow()

    // 號碼族譜狀態
    private val _phoneGenealogyState = MutableStateFlow<PhoneGenealogyUiState>(PhoneGenealogyUiState.Idle)
    val phoneGenealogyState = _phoneGenealogyState.asStateFlow()

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

    // --- 新聞資料狀態 ---
    private val _latestNews = MutableStateFlow<List<com.example.scamdetectorapp.data.repository.NewsItem>>(emptyList())
    val latestNews = _latestNews.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading = _isNewsLoading.asStateFlow()

    private var newsJob: Job? = null

    // --- 已安裝 App 清單（防護 App 管理用）---
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    private val _isInstalledAppsLoading = MutableStateFlow(false)
    val isInstalledAppsLoading = _isInstalledAppsLoading.asStateFlow()

    private var installedAppsJob: Job? = null

    init {
        updatePermissionStatus()
        syncShareComponentState()
        observePermissionAndProtection()
        // App 開啟時即預載新聞與已安裝 App 清單，進入頁面時可直接顯示
        refreshNews()
        loadInstalledApps()
    }

    private fun observePermissionAndProtection() {
        viewModelScope.launch {
            // 等待首次權限檢查完成，避免啟動時因為初始值為 false 而誤關閉防護
            permissionManager.updatePermissionStatus()

            permissionStatus.collect { _ ->
                // 檢查核心權限
                if (!hasDisplayPermissions() && isProtectionEnabled.value) {
                    DebugLog.d("MainViewModel", "Core permissions lost, disabling protection")
                    settingsManager.setProtectionEnabled(false)
                }
            }
        }
    }
    /**
     * 更新新聞。已有快取時於背景靜默更新，失敗則保留快取；尚無資料時才顯示錯誤項目。
     * 若已有更新進行中則略過，避免啟動預載與進入頁面時重複請求。
     */
    fun refreshNews() {
        if (newsJob?.isActive == true) return
        newsJob = viewModelScope.launch {
            _isNewsLoading.value = true
            try {
                com.example.scamdetectorapp.data.repository.NewsRepository.fetchAllLatestNews()
                    .onSuccess { _latestNews.value = it }
                    .onFailure { e ->
                        if (_latestNews.value.isEmpty()) {
                            _latestNews.value = listOf(
                                com.example.scamdetectorapp.data.repository.NewsRepository.errorNewsItem(e)
                            )
                        } else {
                            Log.w("MainViewModel", "News refresh failed, keeping cached news: ${e.message}")
                        }
                    }
            } finally {
                _isNewsLoading.value = false
            }
        }
    }

    /**
     * 於背景解析所有可啟動 App 的名稱與圖示，並依名稱排序後快取。
     */
    fun loadInstalledApps() {
        if (installedAppsJob?.isActive == true) return
        installedAppsJob = viewModelScope.launch {
            _isInstalledAppsLoading.value = true
            try {
                _installedApps.value = withContext(Dispatchers.IO) { queryInstalledApps() }
                DebugLog.d("MainViewModel", "Preloaded ${_installedApps.value.size} installed apps")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load installed apps", e)
            } finally {
                _isInstalledAppsLoading.value = false
            }
        }
    }

    private fun queryInstalledApps(): List<InstalledAppInfo> {
        val context = getApplication<Application>()
        val packageManager = context.packageManager
        // 圖示以列表顯示尺寸 (32dp) 縮放後快取，降低記憶體佔用
        val iconSizePx = (APP_ICON_SIZE_DP * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .mapNotNull { appInfo ->
                try {
                    InstalledAppInfo(
                        packageName = appInfo.packageName,
                        label = packageManager.getApplicationLabel(appInfo).toString(),
                        icon = appInfo.loadIcon(packageManager)
                            .toBitmap(iconSizePx, iconSizePx)
                            .asImageBitmap()
                    )
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Skip app ${appInfo.packageName}: ${e.message}")
                    null
                }
            }
            .sortedBy { it.label }
    }

    val allHistory: StateFlow<List<HistoryEntity>> = (repository.getAllHistory()
        ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allPhoneHistory: StateFlow<List<PhoneHistoryEntity>> = (repository.getAllPhoneHistory()
        ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 儀表板統計數據
    val dashboardStats: StateFlow<DashboardStats> = combine(allHistory, allPhoneHistory) { history, phoneHistoryList ->
        val highRiskCount = history.count { it.riskLevel == "HIGH" }

        // 今日 (00:00 起) 的高風險筆數，供「今日風險趨勢」摘要使用
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayHighRiskCount = history.count { it.riskLevel == "HIGH" && it.timestamp >= todayStart }

        // 詐騙類型分佈比例 (按媒介區分)
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

        // 詐騙電話種類統計 (來自 PhoneHistoryEntity 與 detection_history 電話紀錄加總)
        val defaultDetailCategories = listOf("約會交友", "假包裹釣魚", "假投資", "假求職", "假信貸", "假冒公務", "假冒電商", "商業騷擾", "其他")
        val detailColors = listOf(
            Color(0xFFF05A5A), Color(0xFFFFA905), Color(0xFFF2C94C), 
            Color(0xFF00C853), Color(0xFF4F7CFF), Color(0xFF4B0082), 
            Color(0xFFA78BFA), Color(0xFF64748B), Color(0xFFB0BEC5)
        )
        
        val phoneTypeRecords = mutableListOf<String>()
        phoneHistoryList.forEach { phoneTypeRecords.add(it.phoneType) }
        highRiskHistory.filter { it.type == "電話" }.forEach { 
            it.category?.let { cat -> phoneTypeRecords.add(cat) }
        }

        val totalPhoneCount = phoneTypeRecords.size
        
        val phoneTypeDistribution = defaultDetailCategories.mapIndexed { idx, category ->
            val count = if (category == "其他") {
                phoneTypeRecords.count { type ->
                    defaultDetailCategories.dropLast(1).none { cat -> matchesPhoneCategory(type, cat) }
                }
            } else {
                phoneTypeRecords.count { type ->
                    matchesPhoneCategory(type, category)
                }
            }

            val percentage = if (totalPhoneCount > 0) {
                Math.round(count * 100.0f / totalPhoneCount)
            } else 0

            ScamTypeRatio(
                label = category,
                percentage = percentage,
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
            highRiskMessages = todayHighRiskCount,
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
            DebugLog.d("MainViewModel", "Share component enabled: $enabled")
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

    /**
     * 號碼族譜：載入指定號碼的關聯資料
     */
    fun loadPhoneGenealogy(phoneNumber: String) {
        _phoneGenealogyState.value = PhoneGenealogyUiState.Loading
        viewModelScope.launch {
            repository.getPhoneGenealogy(phoneNumber.trim()).fold(
                onSuccess = { data: PhoneGenealogyData ->
                    _phoneGenealogyState.value = PhoneGenealogyUiState.Success(data)
                },
                onFailure = { e: Throwable ->
                    _phoneGenealogyState.value = PhoneGenealogyUiState.Error(
                        message = e.message ?: "讀取號碼關聯資料失敗",
                        title = "族譜載入失敗"
                    )
                }
            )
        }
    }

    fun resetPhoneGenealogy() {
        _phoneGenealogyState.value = PhoneGenealogyUiState.Idle
    }

    /**
     * 電話詐騙回報：成功時回傳顯示給使用者的訊息
     */
    suspend fun reportPhone(phoneNumber: String, phoneType: String, otherType: String? = null): Result<String> {
        return repository.reportPhone(
            phoneNumber = phoneNumber.trim(),
            phoneType = phoneType,
            otherType = otherType
        ).fold(
            onSuccess = { report -> Result.success(report.message ?: "可疑電話號碼已回報") },
            onFailure = { e ->
                Log.e("MainViewModel", "Report phone failed", e)
                val message = when (e) {
                    is HttpException -> "伺服器錯誤 (${e.code()})，請稍後再試"
                    is SocketTimeoutException -> "伺服器回應太慢，請稍後再試"
                    else -> e.message ?: "回報失敗"
                }
                Result.failure(Exception(message, e))
            }
        )
    }

    private fun matchesPhoneCategory(rawType: String, category: String): Boolean {
        val type = rawType.trim()
        return type == category
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

            if (mode == DetectionMode.PHONE && uiModel.riskLevel == "HIGH") {
                val phoneType = uiModel.detailMap?.get("電話類型")?.toString() ?: "其他"
                repository.savePhoneHistory(
                    PhoneHistoryEntity(
                        phoneNumber = content,
                        status = "black",
                        phoneType = phoneType
                    )
                )
            }
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

        val baseScore = result.score?.toIntOrNull() ?: when (rLevel.uppercase()) {
            "HIGH" -> 85
            "MEDIUM" -> 60
            "LOW" -> 20
            "SAFE" -> 10
            else -> 0
        }

        val calculatedScore = if (mode == DetectionMode.PHONE) {
            val reports = result.detailInfo?.get("回報次數")?.toString()?.toIntOrNull() ?: 0
            if (reports > 0) (baseScore + reports * 5).coerceAtMost(100) else baseScore
        } else baseScore

        val finalScore = if (rLevel.uppercase() == "UNKNOWN") 0 else calculatedScore

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
                if (mode != DetectionMode.PHONE) {
                    reasons.add("風險等級: $rLevel")
                    result.threatType?.takeIf { it.isNotEmpty() }?.let { reasons.add("類型: $it") }
                    result.suggestion?.takeIf { it.isNotEmpty() }?.let { reasons.add(it) }
                    if (reasons.size == 1) {
                        reasons.add("暫無此紀錄")
                    }
                } else {
                    reasons.add("暫無此紀錄")
                }
            }
        }

        return ScanUiModel(
            isSafe = (rLevel == "SAFE" || rLevel == "NODATA"),
            riskLevel = rLevel,
            score = finalScore,
            title = title,
            reasons = reasons,
            mode = mode,
            detailMap = result.detailInfo,
            metadata = result.metadata
        )
    }

    companion object {
        private const val APP_ICON_SIZE_DP = 32

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
