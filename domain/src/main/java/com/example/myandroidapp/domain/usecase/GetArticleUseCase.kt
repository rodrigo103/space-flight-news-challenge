package com.example.myandroidapp.domain.usecase

import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class GetArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository,
) {
    companion object {
        private const val REQUEST_TIMEOUT_MS = 30_000L
    }

    suspend operator fun invoke(articleId: Int): Result<Article> {
        val networkResult = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            repository.getArticle(articleId)
        }

        val article = networkResult?.getOrNull()
        if (article != null) return Result.success(article)

        val cached = repository.getCachedArticle(articleId)
        if (cached != null) return Result.success(cached)

        return networkResult ?: Result.failure(Exception("Request timed out"))
    }
}
