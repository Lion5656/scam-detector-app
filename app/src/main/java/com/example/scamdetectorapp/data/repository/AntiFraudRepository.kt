package com.example.scamdetectorapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.scamdetectorapp.data.model.*
import com.example.scamdetectorapp.data.remote.RetrofitClient
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.domain.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri

class AntiFraudRepository(private val context: Context? = null) {
    private val api = RetrofitClient.instance

    suspend fun scan(mode: DetectionMode, input: String): Result<ScanResult> = withContext(Dispatchers.IO) {
        try {
            val result = when (mode) {
                DetectionMode.PHONE -> {
                    val response = api.getData(phoneNumber = input)
                    if (response.success) {
                        val data = response.data
                        val riskLevel = data?.riskLevel ?: "NODATA"
                        ScanResult(
                            riskLevel = riskLevel,
                            description = data?.description
                        )
                    } else throwApiException(response.toString())
                }
                DetectionMode.URL -> {
                    var urlToCheck = input.trim()
                    if (!urlToCheck.startsWith("http://") && !urlToCheck.startsWith("https://")) {
                        urlToCheck = "https://$urlToCheck"
                    }
                    val response = api.getUrlCheck(url = urlToCheck)
                    if (response.success) {
                        val data = response.data
                        val riskLevel = data?.riskLevel ?: "NODATA"
                        ScanResult(
                            riskLevel = riskLevel,
                            description = data?.description,
                            threatType = data?.threatType
                        )
                    } else throwApiException(response.toString())
                }
                DetectionMode.TEXT -> {
                    val response = api.postAiCheck(body = AiCheckRequest(text = input))
                    if (response.success) {
                        val data = response.data
                        val riskLevel = data?.riskLevel ?: "NODATA"
                        ScanResult(
                            riskLevel = riskLevel,
                            description = data?.description,
                            suggestion = data?.suggestion
                        )
                    } else throwApiException(response.toString())
                }
                DetectionMode.PRICE -> {
                    if (context == null || !input.startsWith("uri:")) {
                        // 如果沒有 Context 或格式不對，走模擬成功流程以便測試 UI
                        delay(2000)
                        ScanResult(
                            riskLevel = "SAFE",
                            description = "模擬檢測：圖片格式不支援或環境異常",
                            suggestion = "請檢查相機權限。"
                        )
                    } else {
                        val uri = input.removePrefix("uri:").toUri()
                        val file = uriToFile(context, uri) ?: throw Exception("無法處理圖片檔案")
                        
                        // 轉換為後端可識別的 Multipart 格式
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                        
                        try {
                            val response = api.postPriceCheck(body)
                            if (response.success) {
                                val data = response.data
                                ScanResult(
                                    riskLevel = data?.riskLevel ?: "SAFE",
                                    description = data?.description ?: "商品分析完成",
                                    suggestion = data?.suggestion
                                )
                            } else {
                                ScanResult(
                                    riskLevel = "SAFE",
                                    description = "API 回傳錯誤，目前以模擬結果呈現 (FastAPI 測試中)",
                                    suggestion = "請檢查後端日誌。"
                                )
                            }
                        } catch (e: Exception) {
                            // 網路斷線等異常，回傳模擬結果
                            delay(1000)
                            ScanResult(
                                riskLevel = "SAFE",
                                description = "目前為離線模擬模式 (FastAPI 連線失敗)",
                                suggestion = "後端位置: ${api.javaClass.simpleName}"
                            )
                        }
                    }
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun throwApiException(response: String): Nothing {
        throw Exception("API 回傳失敗: $response")
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
