package com.example.scamdetectorapp.data.remote

import com.example.scamdetectorapp.data.repository.NewsItem
import retrofit2.http.GET

interface WorkerApi {
    /**
     * 從 Cloudflare Workers 獲取統一格式的防詐新聞
     */
    @GET("/")
    suspend fun getLatestNews(): List<NewsItem>
}
