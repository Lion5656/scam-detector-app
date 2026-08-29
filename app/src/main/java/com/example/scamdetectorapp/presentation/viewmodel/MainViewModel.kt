package com.example.scamdetectorapp.presentation.viewmodel

import android.Manifest
import android.app.Application
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
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

// 權限狀態模型
data class PermissionStatus(
    val hasOverlay: Boolean = false,
    val hasUsageStats: Boolean = false,
    val hasPhoneState: Boolean = false,
    val hasContacts: Boolean = false,
    val hasCallLog: Boolean = false,
)

class MainViewModel(application: Application, private val repository: AntiFraudRepository) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)

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
    private val _permissionStatus = MutableStateFlow(PermissionStatus())
    val permissionStatus = _permissionStatus.asStateFlow()

    init {
        updatePermissionStatus()
    }

    fun updatePermissionStatus() {
        val context = getApplication<Application>()
        val overlay = Settings.canDrawOverlays(context)
        val usage = hasUsageStatPermission(context)
        val phone = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val contacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val callLog = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        
        _permissionStatus.value = PermissionStatus(overlay, usage, phone, contacts, callLog)
    }

    private fun hasUsageStatPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
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

    // 【新增快取】在類別層級只呼叫一次 asStateFlow()
    val urlInput = _urlInput.asStateFlow()
    val phoneInput = _phoneInput.asStateFlow()
    val textInput = _textInput.asStateFlow()
    val priceInput = _priceInput.asStateFlow()

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

    /**
     * 重置所有檢測模式的狀態與輸入內容。
     * 通常用於從首頁重新發起檢測流程時，確保所有分頁都回到初始輸入狀態。
     */
    fun resetAllStates() {
        listOf(DetectionMode.URL, DetectionMode.PHONE, DetectionMode.TEXT, DetectionMode.PRICE).forEach { mode ->
            getMutableState(mode).value = ScanUiState.Idle
            setInput(mode, "")
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
                    
                    // 自動儲存至本地資料庫
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
            // 圖片模式下只存檔名
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

        // 針對電話模式的特殊分數邏輯：如果有回報次數，動態調整分數
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
                // 為了相容性，這裡拋出一個更清楚的錯誤，或提供一個預設的（但通常 Composable 會呼叫 provideFactory）
                throw IllegalStateException("MainViewModel requires Application. Use provideFactory(application) instead.")
            }
        }
    }
}
