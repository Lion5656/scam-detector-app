package com.example.scamdetectorapp.data.repository

/**
 * 新聞資料模型
 */
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
    // 暫時採用硬編碼方式導入資料，未來可引入實作介面獲取資料來源
    val newsList = listOf(
        NewsItem(
            "【查核】網傳連結「填寫7-ELEVEN問卷調查可抽1萬元」？",
            "近日網路流傳一個聲稱「7-ELEVEN問卷調查可抽1萬元」的網址；經查證，台灣7-ELEVEN沒有推出問卷調查抽獎活動，網傳網址也與台灣7-11官網不同，這是詐騙連結。",
            "台灣事實查核中心",
            "https://tfc-taiwan.org.tw/fact-check-reports/taiwan-7-eleven-no-survey-cash-prize-scam-page/",
            "1天前",
            NewsType.NEWS
        ),
        NewsItem(
            "【165警訊】假買家騙賣家詐騙",
            "在臉書上刊登出售球拍的廣告，就隨即有人聯繫表示想購買，並提議使用特定的快遞平臺交易。【防詐重要性】：任何要求透過通訊軟體私下連結「客服」、並以「實名驗證」為由要求轉帳的操作，百分之百是詐騙。",
            "165 全民防詐網",
            "https://165dashboard.tw/city-case-summary",
            "1天前",
            NewsType.TREND
        ),
        NewsItem(
            "Dcard 熱議：假買家利用「簽署金流保障」誘導賣家掃碼後存款遭轉走",
            "網友分享在二手平台賣東西，對方聲稱無法下單並傳來「金流驗證」QR Code，掃描並操作網銀後，帳戶內的數萬元瞬間蒸發。",
            "Dcard 反詐騙板",
            "https://www.dcard.tw/f/anti_fraud",
            "2天前"
        ),
        NewsItem(
            "【查核】LINE 輔助認證是詐騙！點進去你的帳號就會被盜走",
            "親友傳來「幫我點一下輔助認證」？這是在騙取你的簡訊驗證碼。一旦提供，詐騙集團將接管你的 Line 帳號並向其他人借錢。",
            "台灣事實查核中心",
            "https://tfc-taiwan.org.tw/articles/9144",
            "5天前"
        ),
        NewsItem(
            "「交通罰單逾期未繳」簡訊？監理站提醒：網址非 gov.tw 都是假的",
            "最新簡訊詐騙手法：偽造罰單催繳通知。點入後頁面極其逼真，但只要網址結尾不是 .gov.tw，絕對是釣魚網站，請勿輸入卡號。",
            "監理服務網",
            "https://www.mvdis.gov.tw/",
            "1週前"
        ),
        NewsItem(
            "【165警訊】中獎通知要先繳稅？小心演唱會門票詐騙新花招",
            "詐騙者在社群平台發布假抽獎，中獎後要求支付「關稅」或「手續費」。警方提醒：正規抽獎不會要求在領獎前轉帳。",
            "165 全民防詐網",
            "https://165.npa.gov.tw/#/article/news/585",
            "4天前"
        ),
        NewsItem(
            "飆股群組進去了就出不來！網友血淚控訴假投資平台手法",
            "標榜「穩賺不賠」、「老師帶路」。初期給予小額獲利甜頭，待投入鉅款後即以「違約」、「稅金」等理由拒絕出金並消失。",
            "165 全民防詐網",
            "https://165.npa.gov.tw/#/article/news/580",
            "1週前"
        ),
        NewsItem(
            "Threads 分享：最新「假包裹」簡訊，誘導點擊實則安裝惡意程式",
            "網友警告：收到簡訊稱包裹地址不全，點入連結後會跳出下載 App 提示。這類軟體會監聽你的簡訊並盜取網銀密碼。",
            "Threads",
            "https://www.threads.net/",
            "昨天"
        ),
        NewsItem(
            "IG 案例：徵才「打字員」月入五萬？小心變詐騙人頭帳戶",
            "標榜在家工作、無需技術。對方會要求提供存摺、金融卡以發放薪資，實際上卻將你的帳戶作為洗錢轉帳中心。",
            "Instagram @165_npa",
            "https://www.instagram.com/165_npa/",
            "6天前"
        ),
        NewsItem(
            "虛擬貨幣假錢包盜幣：誘導下載非官方 App 導致資產清空",
            "駭客在搜尋引擎投放廣告，引導使用者進入假官網下載錢包。只要輸入助記詞，帳戶內的所有幣種會瞬間歸零。",
            "165 全民防詐網",
            "https://165.npa.gov.tw/",
            "2週前"
        )
    )

    /**
     * 獲取前兩則新聞用於首頁預覽
     */
    fun getPreviewNews() = newsList.take(2)

    /**
     * 從真實 165 API 抓取跑馬燈數據 (包含失敗備援機制)
     */
    suspend fun fetch165TickerMessages(): List<String> {
        val fallbackMessages = listOf(
            "[提醒] 165 提醒：近期假冒「台電」、「自來水公司」欠費簡訊多發，請勿點擊連結。",
            "[數據] 今日全台已攔截逾 3,000 筆涉詐電話與釣魚簡訊。",
            "[熱點] 「解除分期付款」詐騙手法更新，接獲 +886 開頭電話請警覺。",
            "[公告] 刑事局提醒：檢警辦案絕不會要求匯款或監管帳戶。"
        )

        return try {
            // 使用 withTimeout 避免政府伺服器回應過慢導致 UI 沒反應
            kotlinx.coroutines.withTimeout(5000) {
                val response = com.example.scamdetectorapp.data.remote.RetrofitClient.oneSixFiveInstance.getRumors()
                if (response.success && response.result.records.isNotEmpty()) {
                    response.result.records.map { "[最新闢謠] ${it.title}" }
                } else {
                    fallbackMessages
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NewsRepository", "165 API Connection Failed: ${e.message}")
            // 連線失敗（如伺服器維護、網路限制）時，直接回傳精選備援訊息
            fallbackMessages
        }
    }
}
