package com.example.myandroidapp.ui.articles.list

/**
 * Contrato MVI de la pantalla de lista.
 *
 * - [ArticlesListIntent]: **único punto de entrada** de la View (`onIntent`).
 * - [ArticlesListResult]: salidas del trabajo asíncrono que consume el reducer.
 * - [ArticlesListEffect]: eventos **one-shot** (navegación) que no forman parte del estado.
 * - [ArticlesListUiState]: el **único estado inmutable** de la pantalla.
 */
sealed interface ArticlesListIntent {
    data class SearchQueryChanged(val query: String) : ArticlesListIntent
    data object ClearSearch : ArticlesListIntent
    data class ArticleClicked(val articleId: Int) : ArticlesListIntent
    data class SendAnalytics(val event: String, val properties: Map<String, String>) : ArticlesListIntent
}

sealed interface ArticlesListResult {
    data class SearchQueryUpdated(val query: String) : ArticlesListResult
}

sealed interface ArticlesListEffect {
    data class OpenArticle(val articleId: Int) : ArticlesListEffect
}

data class ArticlesListUiState(
    val searchQuery: String = "",
)
