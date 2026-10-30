package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel MVI del pane (tablet): mismo contrato de estado/resultados que la pantalla de detalle,
 * pero el artículo se pide con un intent explícito ([ArticleDetailPaneIntent.Load]) porque acá no
 * hay `SavedStateHandle`. Sin efectos one-shot: el pane no navega.
 */
@HiltViewModel
class ArticleDetailPaneViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    private val _state = MutableStateFlow<ArticleDetailUiState>(ArticleDetailUiState.Loading)
    val state: StateFlow<ArticleDetailUiState> = _state.asStateFlow()

    private val intents = Channel<ArticleDetailPaneIntent>(Channel.UNLIMITED)
    private val results = Channel<ArticleDetailResult>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            intents.consumeAsFlow().collect { intent -> handleIntent(intent) }
        }
        viewModelScope.launch {
            results.consumeAsFlow().collect { result -> _state.update { reduce(it, result) } }
        }
    }

    /** Único punto de entrada: la View solo puede pedir cosas por acá. */
    fun onIntent(intent: ArticleDetailPaneIntent) {
        intents.trySend(intent)
    }

    private suspend fun handleIntent(intent: ArticleDetailPaneIntent) {
        when (intent) {
            is ArticleDetailPaneIntent.Load -> load(intent.articleId)
        }
    }

    private suspend fun load(articleId: Int) {
        analytics.logScreenView("ArticleDetail_$articleId")
        results.send(ArticleDetailResult.Loading)
        getArticle(articleId)
            .onSuccess { article ->
                analytics.logEvent("article_loaded", mapOf("id" to article.id.toString()))
                results.send(ArticleDetailResult.Loaded(article))
            }
            .onFailure { e ->
                analytics.logError(e, "loadArticle_$articleId")
                results.send(ArticleDetailResult.Failed(e.message ?: "Unknown error"))
            }
    }
}
