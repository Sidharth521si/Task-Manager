package com.example.practiceee.repository


import com.example.practiceee.data.NewsResponse
import com.example.practiceee.network.NewsApiService

class NewsRepository(private val api: NewsApiService) {
    suspend fun getNews(apiKey: String): NewsResponse {
        return api.getTopHeadlines(apiKey = apiKey)
    }
}
