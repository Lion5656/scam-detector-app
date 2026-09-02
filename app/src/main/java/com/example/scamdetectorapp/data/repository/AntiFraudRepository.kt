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
import kotlinx.coroutines.flow.Flow

class AntiFraudRepository(private val context: Context? = null) {
    private val api = RetrofitClient.instance
    private val db = context?.let { AppDatabase.getDatabase(it) }
    private val historyDao = db?.historyDao()

    suspend fun saveHistory(history: HistoryEntity) {
        historyDao?.insert(history)
    }

    fun getAllHistory(): Flow<List<HistoryEntity>>? {
        return historyDao?.getAllHistory()
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
                        if (data?.status == "white"){
                            riskLevel = "SAFE"
                        }
                        else if (data?.status == "black"){
                            riskLevel = "HIGH"
                        }
                        else {
                            riskLevel = "UNKNOWN"
                        }
                        ScanResult(
                            riskLevel = riskLevel,
                            threatType = data?.phoneType,
                            suggestion = if(riskLevel == "SAFE") "此號碼目前尚未檢測出風險" else "危險號碼，請不要進行撥打操作",
                            detailInfo = mutableMapOf<String, Any>().apply {
                                data?.phoneType?.let { put("電話類型", it) }
                                data?.firstReportedAt?.let {put("首次回報", it) }
                                data?.lastReportedAt?.let { put("最後回報", it) }
                                data?.totalReports?.let {put("回報次數", it.toString())}
                                data?.ownerName?.let { put("擁有者", it) }
                            }
                        )
                    } else throw Exception("API 回傳失敗: ${response.version}")
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
                    } else throw Exception("API 回傳失敗: ${response.version}")
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
                    } else throw Exception("API 回傳失敗: ${response.version}")
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
                            throw Exception("API 分析失敗: ${response.version}")
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
