package com.example.myandroidapp.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.myandroidapp.data.ApiService
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(ExperimentalPagingApi::class)
class ArticleRemoteMediator(
    private val apiService: ApiService,
    private val articleDao: ArticleDao,
) : RemoteMediator<Int, ArticleEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ArticleEntity>,
    ): MediatorResult {
        val offset = when (loadType) {
            LoadType.REFRESH -> 0
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val lastItem = state.lastItemOrNull()
                if (lastItem == null) 0
                else state.pages.sumOf { it.data.size }
            }
        }

        return try {
            val response = apiService.getArticles(limit = PAGE_SIZE, offset = offset)
            val articles = response.results
            articleDao.insertAll(articles.map { it.toEntity() })
            MediatorResult.Success(endOfPaginationReached = articles.size < PAGE_SIZE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnknownHostException) {
            MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: ConnectException) {
            MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: SocketTimeoutException) {
            MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
