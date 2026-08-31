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
import com.example.scamdetectorapp.data.local.HistoryEntity
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

    // 暴露歷史紀錄 Flow 給 UI
    val allHistory: StateFlow<List<HistoryEntity>> = repository.getAllHistory()
        ?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        ?: MutableStateFlow(emptyList())

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
                DetectionMode.URL -> "URL"
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
                score = uiModel.score
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
