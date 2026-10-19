package com.example.myandroidapp.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.myandroidapp.data.local.ArticleDao
import com.example.myandroidapp.data.local.ArticleRemoteMediator
import com.example.myandroidapp.data.mappers.toArticle
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import kotlinx.coroutines.CancellationException
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
    // Sin wrapper de resultado: devuelve el valor y relanza la excepción tras loguearla.
    // No se usa `runCatching` (ni `Result`): atraparía también CancellationException.
    override suspend fun getArticles(limit: Int, offset: Int): List<Article> =
        try {
            apiService.getArticles(limit = limit, offset = offset).results
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error fetching articles")
            throw e
        }

    override suspend fun searchArticles(query: String, limit: Int): List<Article> =
        try {
            apiService.getArticles(limit = limit, offset = 0, search = query).results
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error searching articles with query: %s", query)
            throw e
        }

    override suspend fun getArticle(id: Int): Article =
        try {
            apiService.getArticle(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error fetching article with id: %d", id)
            throw e
        }

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

    private companion object {
        const val PAGE_SIZE = 20
    }
}
