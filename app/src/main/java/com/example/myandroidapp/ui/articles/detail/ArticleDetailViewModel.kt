package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de presentación que consume la pantalla de detalle (contrato visual existente). */
data class ArticleDetailState(
    val article: Article,
)

/**
 * ViewModel MVI del detalle.
 *
 * `View -> onIntent() -> Channel<Intent> -> trabajo -> Channel<Result> -> reducer puro ->
 * StateFlow<UiState>`, con `NavigateBack` como efecto one-shot por un canal aparte.
 */
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val articleId: Int =
        checkNotNull(savedStateHandle["articleId"]) { "articleId required" }

    private val _state = MutableStateFlow<ArticleDetailUiState>(ArticleDetailUiState.Loading)
    val state: StateFlow<ArticleDetailUiState> = _state.asStateFlow()

    private val _effects = Channel<ArticleDetailEffect>(Channel.BUFFERED)
    val effects: Flow<ArticleDetailEffect> = _effects.receiveAsFlow()

    private val intents = Channel<ArticleDetailIntent>(Channel.UNLIMITED)
    private val results = Channel<ArticleDetailResult>(Channel.UNLIMITED)

    init {
        analytics.logScreenView("ArticleDetail_$articleId")
        viewModelScope.launch {
            intents.consumeAsFlow().collect { intent -> handleIntent(intent) }
        }
        viewModelScope.launch {
            results.consumeAsFlow().collect { result -> _state.update { reduce(it, result) } }
        }
        onIntent(ArticleDetailIntent.Retry)
    }

    /** Único punto de entrada: la View solo puede pedir cosas por acá. */
    fun onIntent(intent: ArticleDetailIntent) {
        intents.trySend(intent)
    }

    private suspend fun handleIntent(intent: ArticleDetailIntent) {
        when (intent) {
            ArticleDetailIntent.Retry -> load()

            ArticleDetailIntent.Delete -> {
                if (_state.value !is ArticleDetailUiState.Content) return
                analytics.logEvent("article_deleted", mapOf("id" to articleId.toString()))
                _effects.send(ArticleDetailEffect.NavigateBack)
            }
        }
    }

    private suspend fun load() {
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
