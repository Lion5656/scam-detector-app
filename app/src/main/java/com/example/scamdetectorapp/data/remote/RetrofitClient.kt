package com.example.scamdetectorapp.data.remote

import com.example.scamdetectorapp.util.DebugLog
import android.util.Log
import com.example.scamdetectorapp.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL
    private const val NEWS_URL = BuildConfig.NEWS_URL
    private const val OPEN_DATA_URL = BuildConfig.OPEN_DATA_URL

    init {
        System.loadLibrary("scamdetectorapp")
        DebugLog.d("RetrofitClient", "Initializing RetrofitClient")
    }

    private external fun getApiKey(): String

    // 金鑰於程序生命週期內不變，只透過 JNI 讀取一次，避免每個請求都跨 JNI 呼叫
    private val cachedApiKey: String by lazy {
        getApiKey().also {
            if (it.isEmpty()) Log.e("RetrofitClient", "Native getApiKey() returned an empty key")
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            DebugLog.d("RetrofitClient", "--> ${original.method} ${original.url}")

            val request = original.newBuilder()
                .header("x-api-key", cachedApiKey)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .method(original.method, original.body)
                .build()

            val response = chain.proceed(request)
            DebugLog.d("RetrofitClient", "<-- ${response.code} ${original.url}")
            response
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 一個完全乾淨的 OkHttpClient，不帶任何自定義 Header，
     * 專門用於政府開放資料 API 或其他第三方資源。
     */
    private val cleanHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val instance: AntiFraudApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 主線 API 需要 API Key
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AntiFraudApi::class.java)
    }

    /**
     * Cloudflare Workers 專用的 Retrofit 實例
     */
    val newsApiService: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(NEWS_URL)
            .client(cleanHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }

    /**
     * 165 政府開放資料專用的 Retrofit 實例 (使用不同的 BaseURL 且不帶 API Key)
     */
    val oneSixFiveInstance: OneSixFiveApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPEN_DATA_URL)
            .client(cleanHttpClient) // 使用乾淨的連線器，避免被政府伺服器擋掉
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OneSixFiveApi::class.java)
    }
}
