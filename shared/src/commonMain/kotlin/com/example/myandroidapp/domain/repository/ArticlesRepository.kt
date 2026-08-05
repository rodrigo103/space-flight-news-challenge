package com.example.myandroidapp.domain.repository

import androidx.paging.PagingData
import com.example.myandroidapp.domain.model.Article
import kotlinx.coroutines.flow.Flow

interface ArticlesRepository {
    suspend fun getArticles(limit: Int = 20, offset: Int = 0): Result<List<Article>>

    suspend fun searchArticles(query: String, limit: Int = 20): Result<List<Article>>

    suspend fun getArticle(id: Int): Result<Article>

    suspend fun getCachedArticle(id: Int): Article?

    fun getArticlesPaged(searchQuery: String? = null): Flow<PagingData<Article>>
}
