package com.example.myandroidapp.domain.usecase

import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.data.remote.getOrNull
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import kotlinx.coroutines.withTimeoutOrNull

class GetArticleUseCase(
    private val repository: ArticlesRepository,
) {
    companion object {
        private const val REQUEST_TIMEOUT_MS = 30_000L
    }

    suspend operator fun invoke(articleId: Int): ApiResult<Article> {
        val networkResult = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            repository.getArticle(articleId)
        }

        networkResult?.getOrNull()?.let { return ApiResult.Success(it) }

        val cached = repository.getCachedArticle(articleId)
        if (cached != null) return ApiResult.Success(cached)

        return networkResult ?: ApiResult.Failure(IllegalStateException("Request timed out"))
    }
}
