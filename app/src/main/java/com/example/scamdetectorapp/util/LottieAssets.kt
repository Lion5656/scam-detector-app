package com.example.scamdetectorapp.util

import android.content.Context
import android.util.Log
import com.airbnb.lottie.LottieCompositionFactory

/**
 * App 內使用的 Lottie 動畫檔名，並提供啟動時預載。
 *
 * rememberLottieComposition(LottieCompositionSpec.Asset(name)) 預設快取鍵與
 * LottieCompositionFactory.fromAsset(context, name) 相同，預載後頁面開啟時可直接命中快取，
 * 不必等待解析 .lottie 檔，避免動畫區塊先空白再出現。
 */
object LottieAssets {
    const val SPLASH = "ECM - Nominal Case.lottie"
    const val HOME_ROBOT = "Robot assistant  Online manager.lottie"
    const val DASHBOARD_BACKGROUND = "Background Full Screen-Night.lottie"
    const val LOADING = "loading.lottie"

    private val ALL = listOf(SPLASH, HOME_ROBOT, DASHBOARD_BACKGROUND, LOADING)

    /** 於背景執行緒開始解析所有動畫並寫入 Lottie 快取，不阻塞呼叫端 */
    fun preload(context: Context) {
        val appContext = context.applicationContext
        ALL.forEach { name ->
            LottieCompositionFactory.fromAsset(appContext, name)
                .addFailureListener { e -> Log.w("LottieAssets", "預載動畫失敗: $name", e) }
        }
    }
}
