package com.example.myandroidapp.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.data.local.ArticleDao
import com.example.myandroidapp.data.local.ArticleRemoteMediator
import com.example.myandroidapp.data.mappers.toArticle
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultArticlesRepository @Inject constructor(
    private val apiService: ApiService,
    private val articleDao: ArticleDao,
) : ArticlesRepository {
    override suspend fun getArticles(limit: Int, offset: Int): Result<List<Article>> =
        runCatching {
            apiService.getArticles(limit = limit, offset = offset).results
        }.onFailure {
            Timber.e(it, "Error fetching articles")
        }

    override suspend fun searchArticles(query: String, limit: Int): Result<List<Article>> =
        runCatching {
            apiService.getArticles(limit = limit, offset = 0, search = query).results
        }.onFailure {
            Timber.e(it, "Error searching articles with query: %s", query)
        }

    override suspend fun getArticle(id: Int): Result<Article> =
        runCatching {
            apiService.getArticle(id)
        }.onFailure {
            Timber.e(it, "Error fetching article with id: %d", id)
        }

    override suspend fun getCachedArticle(id: Int): Article? =
        articleDao.getById(id)?.toArticle()

    @OptIn(ExperimentalPagingApi::class)
    override fun getArticlesPaged(searchQuery: String?): Flow<PagingData<Article>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = if (searchQuery.isNullOrBlank()) {
                ArticleRemoteMediator(apiService, articleDao)
            } else null,
            pagingSourceFactory = {
                if (searchQuery.isNullOrBlank()) {
                    articleDao.pagingSource()
                } else {
                    articleDao.searchPagingSource(searchQuery)
                }
            },
        ).flow.map { pagingData -> pagingData.map { it.toArticle() } }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
