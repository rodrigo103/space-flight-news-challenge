package com.example.myandroidapp.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.myandroidapp.data.local.ArticleDao
import com.example.myandroidapp.data.local.ArticleRemoteMediator
import com.example.myandroidapp.data.mappers.toArticle
import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.data.remote.apiRunCatching
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import com.example.myandroidapp.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultArticlesRepository(
    private val apiService: ApiService,
    private val articleDao: ArticleDao,
) : ArticlesRepository {

    override suspend fun getArticles(limit: Int, offset: Int): ApiResult<List<Article>> =
        apiRunCatching {
            apiService.getArticles(limit = limit, offset = offset).results
        }.also { it.logIfFailure("Error fetching articles") }

    override suspend fun searchArticles(query: String, limit: Int): ApiResult<List<Article>> =
        apiRunCatching {
            apiService.getArticles(limit = limit, offset = 0, search = query).results
        }.also { it.logIfFailure("Error searching articles with query: $query") }

    override suspend fun getArticle(id: Int): ApiResult<Article> =
        apiRunCatching {
            apiService.getArticle(id)
        }.also { it.logIfFailure("Error fetching article with id: $id") }

    override suspend fun getCachedArticle(id: Int): Article? =
        articleDao.getById(id)?.toArticle()

    @OptIn(ExperimentalPagingApi::class)
    override fun getArticlesPaged(searchQuery: String?): Flow<PagingData<Article>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = ArticleRemoteMediator(apiService, articleDao, searchQuery?.ifBlank { null }),
            pagingSourceFactory = {
                if (searchQuery.isNullOrBlank()) {
                    articleDao.pagingSource()
                } else {
                    articleDao.searchPagingSource(searchQuery)
                }
            },
        ).flow.map { pagingData -> pagingData.map { it.toArticle() } }
    }

    private fun ApiResult<*>.logIfFailure(message: String) {
        if (this is ApiResult.Failure) AppLogger.e(error, message)
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
