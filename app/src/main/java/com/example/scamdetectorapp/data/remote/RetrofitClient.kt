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
            // 檢查 Key 是否成功從 C++ 取得
            if (apiKey.isEmpty()) {
                android.util.Log.e("RetrofitClient", "ERROR: Native getApiKey() returned EMPTY string!")
            } else {
                android.util.Log.d("RetrofitClient", "Native getApiKey() success. Length: ${apiKey.length}")
            }

            val original = chain.request()
            val request = original.newBuilder()
                .header("x-api-key", apiKey)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
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
