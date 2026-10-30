package com.example.myandroidapp.ui.articles.detail

import com.example.myandroidapp.domain.model.Article

/**
 * Contrato MVI compartido por la pantalla de detalle (phone) y el pane (tablet).
 *
 * - [ArticleDetailIntent] / [ArticleDetailPaneIntent]: **único punto de entrada** de cada View.
 * - [ArticleDetailResult]: salidas del trabajo asíncrono que consume el reducer.
 * - [ArticleDetailEffect]: eventos **one-shot** (volver atrás) que no forman parte del estado.
 * - [ArticleDetailUiState]: el **único estado inmutable** de la pantalla.
 */
sealed interface ArticleDetailIntent {
    data object Retry : ArticleDetailIntent
    data object Delete : ArticleDetailIntent
}

sealed interface ArticleDetailPaneIntent {
    data class Load(val articleId: Int) : ArticleDetailPaneIntent
}

sealed interface ArticleDetailResult {
    data object Loading : ArticleDetailResult
    data class Loaded(val article: Article) : ArticleDetailResult
    data class Failed(val message: String) : ArticleDetailResult
}

sealed interface ArticleDetailEffect {
    data object NavigateBack : ArticleDetailEffect
}

sealed interface ArticleDetailUiState {
    data object Loading : ArticleDetailUiState
    data class Content(val article: Article) : ArticleDetailUiState
    data class Error(val message: String) : ArticleDetailUiState
}
