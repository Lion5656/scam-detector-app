package com.example.scamdetectorapp.data.remote

import com.example.scamdetectorapp.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    init {
        System.loadLibrary("scamdetectorapp")
        // 加入 Log 以便除錯 (請在 Logcat 搜尋 "RetrofitClient")
        android.util.Log.d("RetrofitClient", "Initializing RetrofitClient")
    }

    private external fun getApiKey(): String

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val apiKey = getApiKey()
            val original = chain.request()
            
            // 加入 Debug Log 協助確認 Header 是否成功帶入
            android.util.Log.d("RetrofitClient", "--> Sending Request to: ${original.url}")
            android.util.Log.d("RetrofitClient", "Using API_KEY: ${if(apiKey.isEmpty()) "EMPTY!" else "Loaded (Length: ${apiKey.length})"}")

            val request = original.newBuilder()
                .header("x-api-key", apiKey)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .method(original.method, original.body)
                .build()
            
            val response = chain.proceed(request)
            android.util.Log.d("RetrofitClient", "<-- Received Response: ${response.code}")
            response
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: AntiFraudApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AntiFraudApi::class.java)
    }

    /**
     * 165 政府開放資料專用的 Retrofit 實例 (使用不同 BASE_URL)
     */
    val oneSixFiveInstance: OneSixFiveApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://od.moi.gov.tw/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OneSixFiveApi::class.java)
    }
}
