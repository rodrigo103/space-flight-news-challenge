package com.example.myandroidapp.domain.usecase

import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeoutException
import javax.inject.Inject

class GetArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository,
) {
    companion object {
        private const val REQUEST_TIMEOUT_MS = 30_000L
    }

    /**
     * Devuelve el artículo de red; si la red falla o se cuelga, cae a caché; si no hay caché,
     * relanza la excepción original (o un timeout si lo que pasó fue el vencimiento del plazo).
     *
     * Sin wrapper de resultado: el camino feliz devuelve `Article` y el fallo es una excepción
     * que decide el borde (el ViewModel la mapea a `UiState.Error`).
     */
    suspend operator fun invoke(articleId: Int): Article {
        var networkError: Throwable? = null

        val networkArticle = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            try {
                repository.getArticle(articleId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                networkError = e
                null
            }
        }

        if (networkArticle != null) return networkArticle

        repository.getCachedArticle(articleId)?.let { return it }

        throw networkError ?: TimeoutException("Request timed out")
    }
}
