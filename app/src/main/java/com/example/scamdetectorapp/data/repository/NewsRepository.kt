package com.example.scamdetectorapp.data.repository

import android.util.Log
import com.example.scamdetectorapp.data.remote.RetrofitClient
import kotlinx.coroutines.*

// 新聞資料模型與枚舉
data class NewsItem(
    val title: String,
    val summary: String,
    val source: String,
    val url: String,
    val date: String,
    val type: NewsType = NewsType.NEWS
)

enum class NewsType {
    NEWS, TREND
}

/**
 * 靜態新聞資料庫
 * 提供全 App 統一的防詐新聞來源
 */
object NewsRepository {

    // 備援新聞資料 (僅當完全無網路且 Worker 失敗時使用)
    val fallbackList = listOf(
        NewsItem(
            "【查核】網傳連結「填寫7-ELEVEN問卷調查可抽1萬元」？",
            "近日網路流傳聲稱問卷抽獎活動，經查證為詐騙釣魚連結。",
            "台灣事實查核中心",
            "https://tfc-taiwan.org.tw/",
            "1天前",
            NewsType.NEWS
        ),
        NewsItem(
            "【165警訊】假買家騙賣家詐騙",
            "臉書刊登出售商品，買家提議使用特定快遞平臺交易並誘導轉帳。",
            "165 全民防詐網",
            "https://165dashboard.tw/",
            "1天前",
            NewsType.TREND
        )
    )

    fun getPreviewNews() = fallbackList.take(2)

    /**
     * 從 Cloudflare Worker 獲取最新即時新聞 (核心方法)
     */
    suspend fun fetchAllLatestNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        Log.d("NewsRepository", "--- 開始連線至 Cloudflare Worker ---")
        try {
            // 直接呼叫 API，不再有任何 NewsAggregator 的殘留
            val remoteNews = RetrofitClient.newsApiService.getLatestNews()
            
            if (remoteNews.isNotEmpty()) {
                Log.d("NewsRepository", "成功取得 ${remoteNews.size} 則新聞")
                // 在標題前加上標記，方便確認是抓到的資料
                remoteNews.map { it.copy(title = "[最新] ${it.title}") }
            } else {
                Log.w("NewsRepository", "Worker 回傳空清單")
                fallbackList
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "連線失敗: ${e.javaClass.simpleName} - ${e.message}")
            
            // 如果連線失敗，返回一個帶有錯誤訊息的項目，讓使用者知道原因
            listOf(
                NewsItem(
                    "暫時無法取得最新新聞",
                    "連線錯誤: ${e.message}。請確認網路狀況或 Worker 部署是否正確。",
                    "連線診斷",
                    "",
                    "現在",
                    NewsType.NEWS
                )
            )
        }
    }

    /**
     * 165 跑馬燈
     */
    suspend fun fetch165TickerMessages(): List<String> {
        val messages = listOf(
            "[提醒] 165 提醒：近期假冒「台電」欠費簡訊多發，請勿點擊。",
            "[熱點] 今日全台已攔截逾 3,000 筆涉詐電話。",
            "[公告] 刑事局提醒：檢警辦案絕不會要求匯款。"
        )
        return try {
            val response = RetrofitClient.oneSixFiveInstance.getRumors()
            if (response.success && response.result.records.isNotEmpty()) {
                response.result.records.map { "[最新闢謠] ${it.title}" }
            } else messages
        } catch (e: Exception) {
            messages
        }
    }
}
