package com.example.scamdetectorapp.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.scamdetectorapp.data.model.*
import com.example.scamdetectorapp.data.remote.RetrofitClient
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.domain.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri
import com.example.scamdetectorapp.data.local.db.AppDatabase
import com.example.scamdetectorapp.data.local.entity.HistoryEntity
import com.example.scamdetectorapp.data.local.entity.PhoneHistoryEntity
import kotlinx.coroutines.flow.Flow
import com.example.scamdetectorapp.presentation.model.GenealogyNode
import com.example.scamdetectorapp.presentation.model.PhoneGenealogyData

class AntiFraudRepository(private val context: Context? = null) {
    private val api = RetrofitClient.instance
    private val db = context?.let { AppDatabase.getDatabase(it) }
    private val historyDao = db?.historyDao()

    companion object {
        private val phoneFraudTypes = setOf(
            "約會交友", "假投資", "假信貸", "假冒公務", "假包裹釣魚", "假求職", "商業騷擾", "假冒電商", "其他"
        )
    }

    suspend fun saveHistory(history: HistoryEntity) {
        historyDao?.insert(history)
    }

    fun getAllHistory(): Flow<List<HistoryEntity>>? {
        return historyDao?.getAllHistory()
    }

    suspend fun savePhoneHistory(phoneHistory: PhoneHistoryEntity) {
        historyDao?.insertPhoneHistory(phoneHistory)
    }

    fun getAllPhoneHistory(): Flow<List<PhoneHistoryEntity>>? {
        return historyDao?.getAllPhoneHistory()
    }

    /**
     * 取得最新 5 筆檢測紀錄
     */
    fun getRecentScans(): Flow<List<HistoryEntity>>? {
        return historyDao?.getRecentHistory(5)
    }

    suspend fun scan(mode: DetectionMode, input: String): Result<ScanResult> = withContext(Dispatchers.IO) {
        try {
            val result = when (mode) {
                DetectionMode.PHONE -> {
                    val response = api.queryPhoneNum(body = PhoneQueryRequest(phoneNumber = input))
                    Log.d("AntiFraudRepository", "response: ${response.data}")
                    if (response.success) {
                        val data = response.data
                        Log.d("AntiFraudRepository", "Phone data: $data")
                        var riskLevel: String
                        val statusLower = data?.status?.lowercase() ?: ""
                        if (statusLower == "white") {
                            riskLevel = "SAFE"
                        } else if (statusLower == "black") {
                            riskLevel = "HIGH"
                        } else {
                            riskLevel = "UNKNOWN"
                        }

                        val familyStatic = data?.familyStatic.orEmpty()
                        val detailInfo = mutableMapOf<String, Any>().apply {
                            put("狀態", statusLower.ifBlank { "unknown" })
                            data?.phoneType?.let { put("電話類型", it) }
                            data?.firstReportedAt?.let { put("首次回報", it) }
                            data?.lastReportedAt?.let { put("最後回報", it) }
                            data?.totalReports?.let { put("回報次數", it.toString()) }
                            data?.ownerName?.let { put("擁有者", it) }
                            put("可回報", data?.canReport ?: true)
                        }

                        val reasons = buildList {
                            when (statusLower) {
                                "black" -> {
                                    add("此號碼已被標記為黑名單")
                                    data?.phoneType?.let { add("詐騙類型：$it") }
                                    data?.totalReports?.let { add("累積回報 $it 次") }
                                    if (familyStatic.isNotEmpty()) {
                                        add("已找到 ${familyStatic.size} 筆疑似關聯號碼")
                                    }
                                }
                                "white" -> {
                                    add("此號碼已存在白名單")
                                    data?.ownerName?.takeIf { it.isNotBlank() }?.let { add("合法機構／商家：$it") }
                                    add("白名單號碼不可再次回報")
                                }
                                else -> {
                                    add("目前尚無此號碼的檢測資料")
                                    add("若曾接獲可疑來電，可直接回報")
                                }
                            }
                        }

                        ScanResult(
                            riskLevel = riskLevel,
                            threatType = data?.phoneType,
                            suggestion = when (statusLower) {
                                "white" -> "此號碼為白名單資料，可放心辨識來源"
                                "black" -> "危險號碼，請不要進行撥打或回撥操作"
                                else -> "此號碼目前尚無資料，建議提高警覺"
                            },
                            detailInfo = detailInfo,
                            metadata = mapOf(
                                "phoneNumber" to (data?.phoneNumber ?: input),
                                "status" to statusLower,
                                "canReport" to (data?.canReport ?: true),
                                "familyStatic" to familyStatic,
                                "reasons" to reasons
                            )
                        )
                    } else {
                        throw Exception(response.error_message ?: "API 回傳失敗: ${response.version}")
                    }
                }
                DetectionMode.URL -> {
                    var url = input.trim()
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    val response = api.analyzeUrl(body = UrlRequest(url = url))
                    if (response.success) {
                        val data = response.data
                        ScanResult(
                            riskLevel = data?.label,
                            suggestion = data?.reason,
                            score = data?.score?.toString()
                        )
                    } else throw Exception(response.error_message ?: "API 回傳失敗: ${response.version}")
                }
                DetectionMode.TEXT -> {
                    val response = api.analyzeText(body = TextRequest(text = input))
                    if (response.success) {
                        val data = response.data
                        ScanResult(
                            riskLevel = data?.label ?: "SAFE",
                            suggestion = data?.reason,
                            score = data?.score?.toString()
                        )
                    } else throw Exception(response.error_message ?: "API 回傳失敗: ${response.version}")
                }
                DetectionMode.PRICE -> {
                    if (context == null) throw Exception("系統環境異常，請重新啟動 App")
                    if (!input.startsWith("uri:")) throw Exception("圖片路徑無效或未選擇圖片")

                    val uri = input.removePrefix("uri:").toUri()
                    val file = uriToFile(context, uri) ?: throw Exception("無法讀取圖片檔案，請檢查權限")
                    
                    // 取得正確的 Content-Type
                    val contentType = context.contentResolver.getType(uri)  ?: ""
                    val mediaType = contentType.toMediaTypeOrNull()
                    
                    try {
                        val requestFile = file.asRequestBody(mediaType)
                        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                        
                        val response = api.analyzePrice(body)
                        if (response.success) {
                            val data = response.data
                            ScanResult(
                                riskLevel = data?.riskLabel,
                                suggestion = data?.result,
                                score = data?.riskScore,
                                detailInfo = mutableMapOf<String, Any>().apply {
                                    data?.productName?.let { put("商品名稱", it) }
                                    data?.condition?.let { put("商品狀態", it) }
                                    data?.listedPrice?.let { put("商品價格", it) }
                                    data?.marketPrice?.let { put("市場價格", it) }
                                    data?.sellerName?.let { put("賣家名稱", it) }
                                    data?.result?.let { put("結果說明", it) }
                                }
                            )
                        } else {
                            throw Exception(response.error_message ?: "API 分析失敗: ${response.version}")
                        }
                    } finally {
                        file.delete() // 確保暫存檔被刪除
                    }
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhoneGenealogy(phoneNumber: String): Result<PhoneGenealogyData> = withContext(Dispatchers.IO) {
        try {
            val response = api.queryPhoneNum(body = PhoneQueryRequest(phoneNumber = phoneNumber))
            if (!response.success) {
                throw Exception(response.error_message ?: "查詢號碼族譜失敗")
            }

            val data = response.data ?: throw Exception("查無號碼資料")
            val status = data.status?.lowercase().orEmpty()
            val nodes = data.familyStatic.mapIndexed { index, item ->
                GenealogyNode(
                    id = index + 1,
                    phoneNumber = item.related_phone ?: "未知號碼",
                    relationship = "靜態特徵",
                    connectionStrength = ((item.weight ?: 0).coerceIn(0, 100) / 100f),
                    lastActive = data.lastReportedAt,
                    reasons = listOfNotNull(item.reason).ifEmpty { listOf("無關聯原因說明") }
                )
            }

            Result.success(
                PhoneGenealogyData(
                    rootNumber = data.phoneNumber ?: phoneNumber,
                    tagId = data.phoneType ?: "無標籤資料",
                    relatedNodes = nodes,
                    status = status
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportPhone(
        phoneNumber: String,
        phoneType: String,
        otherType: String? = null,
        reporterPhone: String? = null,
        transferType: Int = 0,
        transferContent: String? = null
    ): Result<PhoneReportResult> = withContext(Dispatchers.IO) {
        try {
            if (phoneType !in phoneFraudTypes) {
                throw IllegalArgumentException("不支援的詐騙類型：$phoneType")
            }

            val normalizedOtherType = otherType?.trim()?.takeIf { it.isNotEmpty() }
            if (phoneType == "其他" && normalizedOtherType == null) {
                throw IllegalArgumentException("選擇「其他」時，請輸入自訂類型")
            }

            val response = api.reportPhoneNum(
                body = PhoneReportRequest(
                    phoneNumber = phoneNumber,
                    phoneType = phoneType,
                    otherType = normalizedOtherType,
                    reporterPhone = reporterPhone?.trim()?.takeIf { it.isNotEmpty() },
                    transferType = transferType,
                    transferContent = transferContent?.trim()?.takeIf { it.isNotEmpty() }
                )
            )

            if (!response.success) {
                throw Exception(response.error_message ?: "回報失敗")
            }

            Result.success(response.data ?: throw Exception("回報成功但缺少資料"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 將 Content Uri 轉換為實體 File 檔案，以便 Multipart 上傳
     */
    private fun uriToFile(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
