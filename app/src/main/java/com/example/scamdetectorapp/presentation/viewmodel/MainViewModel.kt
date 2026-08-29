package com.example.scamdetectorapp.presentation.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.scamdetectorapp.data.repository.AntiFraudRepository
import com.example.scamdetectorapp.data.ScanHistoryManager
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.domain.model.ScanResult
import com.example.scamdetectorapp.presentation.model.ScanUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException

sealed interface RecentScansUiState {
    object Loading : RecentScansUiState
    object Empty : RecentScansUiState
    data class Success(val scans: List<com.example.scamdetectorapp.data.local.DetectionEntity>) : RecentScansUiState
    data class Error(val message: String) : RecentScansUiState
}

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Loading : ScanUiState
    data class Success(val result: ScanUiModel) : ScanUiState
    data class Error(val message: String, val title: String = "錯誤") : ScanUiState
}

// 修復：增加建構子參數以符合 Factory 的呼叫
class MainViewModel(application: Application, private val repository: AntiFraudRepository) : AndroidViewModel(application) {
    private val historyManager = ScanHistoryManager(application)

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

    private val _recentScansState = MutableStateFlow<RecentScansUiState>(RecentScansUiState.Loading)
    val recentScansState = _recentScansState.asStateFlow()

    init {
        loadRecentScans()
    }

    private fun loadRecentScans() {
        viewModelScope.launch {
            try {
                // 優先嘗試從 Room 讀取 (符合使用者要求的 DAO 語法)
                // 但為了避免 build issue 導致 App 崩潰，我們加上 try-catch 並提供 SharedPreferences 備案
                try {
                    repository.getRecentScans().collect { scans ->
                        _recentScansState.value = if (scans.isEmpty()) {
                            // 若 Room 為空，嘗試讀取舊有的 SharedPreferences 紀錄作為相容
                            loadFromHistoryManager()
                        } else {
                            RecentScansUiState.Success(scans)
                        }
                    }
                } catch (e: Throwable) {
                    // 若 Room 實作遺失 (如 AppDatabase_Impl 不存在)，則降級使用 HistoryManager
                    android.util.Log.e("MainViewModel", "Room 載入失敗，切換至備案儲存: ${e.message}")
                    _recentScansState.value = loadFromHistoryManager()
                }
            } catch (e: Exception) {
                _recentScansState.value = RecentScansUiState.Error(e.message ?: "未知錯誤")
            }
        }
    }

    private fun loadFromHistoryManager(): RecentScansUiState {
        val history = historyManager.getHistory().take(5)
        return if (history.isEmpty()) {
            RecentScansUiState.Empty
        } else {
            // 將 ScanRecord 轉換為 DetectionEntity 以符合 UI 預期
            val converted = history.map { record ->
                com.example.scamdetectorapp.data.local.DetectionEntity(
                    id = 0, // SharedPreferences 紀錄不使用 DB ID
                    type = record.type.name,
                    input = record.input,
                    riskLevel = if (record.result.isSafe) "SAFE" else "DANGEROUS",
                    score = record.result.score,
                    timestamp = record.timestamp
                )
            }
            RecentScansUiState.Success(converted)
        }
    }

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

    fun scan(mode: DetectionMode, input: String) {
        val stateFlow = getMutableState(mode)
        stateFlow.value = ScanUiState.Loading
        
        viewModelScope.launch {
            // --- 開發者模式：強制攔截所有請求並回傳模擬數據 ---
            val FORCE_MOCK = true 

            if (FORCE_MOCK) {
                kotlinx.coroutines.delay(1500) // 模擬掃描動畫時間
                val mockResult = ScanUiModel(
                    isSafe = false,
                    score = 88,
                    title = "高風險威脅 (模擬模式)",
                    reasons = listOf(
                        "偵測到號碼具備多個詐騙集團特徵",
                        "關聯多組已報案的惡意通訊錄",
                        "行為模式分析結果與 G-9412 分群高度吻合"
                    )
                )
                stateFlow.value = ScanUiState.Success(mockResult)
                android.util.Log.w("MainViewModel", "[MOCK] Bypass API request and returned test data.")
                return@launch
            }

            val result = repository.scan(mode, input.trim())
            result.fold(
                onSuccess = { scanResult ->
                    val uiModel = mapToUiModel(scanResult)
                    stateFlow.value = ScanUiState.Success(uiModel)
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

    private fun mapToUiModel(result: ScanResult): ScanUiModel {
        val rLevel = result.riskLevel

        val score = when (rLevel?.uppercase()) {
            "HIGH" -> 85
            "MEDIUM" -> 60
            "LOW" -> 20
            "SAFE" -> 10
            "NODATA" -> 0
            else -> 0
        }

        val reasons = mutableListOf<String>()
        val title: String

        when (rLevel?.uppercase()) {
            "HIGH", "MEDIUM", "LOW" -> {
                title = when (rLevel.uppercase()) {
                    "HIGH" -> "高風險威脅"
                    "MEDIUM" -> "中風險威脅"
                    else -> "低風險威脅"
                }
                reasons.add("風險等級: $rLevel")
                result.description?.takeIf { it.isNotEmpty() }?.let { reasons.add(it) }
                result.threatType?.takeIf { it.isNotEmpty() }?.let { reasons.add("威脅類型: $it") }
                result.suggestion?.takeIf { it.isNotEmpty() }?.let { reasons.add("建議: $it") }
            }
            "SAFE" -> {
                title = "安全內容"
                reasons.add("無詐騙特徵")
                reasons.add("正規網域/號碼/內容")
            }
            else -> { // Catches NODATA and any other case
                title = "查無資料"
                reasons.add("資料庫暫無此紀錄")
            }
        }

        return ScanUiModel(
            isSafe = rLevel == "SAFE" || rLevel == "NODATA",
            score = score,
            title = title,
            reasons = reasons
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
