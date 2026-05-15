package com.example.myandroidapp.data

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

interface ArticlesRepository {
    suspend fun getArticles(limit: Int = 20, offset: Int = 0): Result<List<Article>>

    suspend fun searchArticles(query: String, limit: Int = 20): Result<List<Article>>

    suspend fun getArticle(id: Int): Result<Article>
}

@Singleton
class DefaultArticlesRepository @Inject constructor(
    private val apiService: ApiService,
) : ArticlesRepository {
    override suspend fun getArticles(limit: Int, offset: Int): Result<List<Article>> =
        runCatching {
            val response = apiService.getArticles(limit = limit, offset = offset)
            extractBody(response).results
        }.onFailure {
            Timber.e(it, "Error fetching articles")
        }

    override suspend fun searchArticles(query: String, limit: Int): Result<List<Article>> =
        runCatching {
            val response =
                apiService.getArticles(limit = limit, offset = 0, search = query)
            extractBody(response).results
        }.onFailure {
            Timber.e(it, "Error searching articles with query: %s", query)
        }

    override suspend fun getArticle(id: Int): Result<Article> =
        runCatching {
            val response = apiService.getArticle(id)
            extractBody(response)
        }.onFailure {
            Timber.e(it, "Error fetching article with id: %d", id)
        }

    private fun <T> extractBody(response: retrofit2.Response<T>): T {
        if (response.isSuccessful) {
            return response.body()!!
        }
        throw IOException("HTTP ${response.code()} ${response.message()}")
    }
}
