package com.example.myandroidapp.domain.repository

import androidx.paging.PagingData
import com.example.myandroidapp.domain.model.Article
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a artículos.
 *
 * **Sin wrapper de resultado**: los métodos devuelven el valor y **lanzan excepción** en error
 * (red, HTTP, serialización). Las otras líneas usan un wrapper (`kotlin.Result` en la nativa
 * clásica, `ApiResult` en la KMP); acá se usa el manejo de excepciones idiomático de Kotlin:
 * cada borde decide cómo mapearla (el ViewModel a `UiState.Error`, el use case a fallback de caché).
 */
interface ArticlesRepository {
    suspend fun getArticles(limit: Int = 20, offset: Int = 0): List<Article>

    suspend fun searchArticles(query: String, limit: Int = 20): List<Article>

    suspend fun getArticle(id: Int): Article

    suspend fun getCachedArticle(id: Int): Article?

    fun getArticlesPaged(searchQuery: String? = null): Flow<PagingData<Article>>
}
