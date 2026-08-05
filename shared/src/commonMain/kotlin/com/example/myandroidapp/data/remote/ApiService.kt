package com.example.myandroidapp.data.remote

import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.model.ArticleResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ApiService(
    private val client: HttpClient,
    private val baseUrl: String = BASE_URL,
) {
    suspend fun getArticles(
        limit: Int = DEFAULT_LIMIT,
        offset: Int = 0,
        search: String? = null,
    ): ArticleResponse = client.get("$baseUrl/articles/") {
        parameter("limit", limit)
        parameter("offset", offset)
        search?.let { parameter("search", it) }
    }.body()

    suspend fun getArticle(id: Int): Article =
        client.get("$baseUrl/articles/$id/").body()

    companion object {
        const val DEFAULT_LIMIT = 20
    }
}
