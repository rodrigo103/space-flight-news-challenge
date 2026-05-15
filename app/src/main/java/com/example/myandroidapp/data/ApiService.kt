package com.example.myandroidapp.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("articles/")
    suspend fun getArticles(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
    ): ArticleResponse

    @GET("articles/{id}/")
    suspend fun getArticle(@Path("id") id: Int): Article
}
