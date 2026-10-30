package com.example.myandroidapp.ui.articles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel MVI de la lista.
 *
 * Flujo unidireccional:
 * `View -> onIntent() -> Channel<Intent> (procesado en serie) -> trabajo -> Channel<Result> ->
 * reducer puro -> StateFlow<UiState>`, y los efectos one-shot (navegación) por un canal aparte.
 *
 * Serializar los intents con un [Channel] evita que dos corrutinas pisen el estado a la vez, y el
 * reducer es la **única** vía de mutación del estado.
 */
@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(ArticlesListUiState())
    val state: StateFlow<ArticlesListUiState> = _state.asStateFlow()

    private val _effects = Channel<ArticlesListEffect>(Channel.BUFFERED)
    val effects: Flow<ArticlesListEffect> = _effects.receiveAsFlow()

    private val intents = Channel<ArticlesListIntent>(Channel.UNLIMITED)
    private val results = Channel<ArticlesListResult>(Channel.UNLIMITED)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val articles: Flow<PagingData<Article>> = _state
        .map { it.searchQuery }
        .debounce(SEARCH_DEBOUNCE_MS)
        .flatMapLatest { query -> repository.getArticlesPaged(searchQuery = query.ifBlank { null }) }
        .cachedIn(viewModelScope)

    init {
        analytics.logScreenView("ArticlesList")
        viewModelScope.launch {
            intents.consumeAsFlow().collect { intent -> handleIntent(intent) }
        }
        viewModelScope.launch {
            results.consumeAsFlow().collect { result -> _state.update { reduce(it, result) } }
        }
    }

    /** Único punto de entrada: la View solo puede pedir cosas por acá. */
    fun onIntent(intent: ArticlesListIntent) {
        intents.trySend(intent)
    }

    private suspend fun handleIntent(intent: ArticlesListIntent) {
        when (intent) {
            is ArticlesListIntent.SearchQueryChanged ->
                results.send(ArticlesListResult.SearchQueryUpdated(intent.query))

            ArticlesListIntent.ClearSearch ->
                results.send(ArticlesListResult.SearchQueryUpdated(""))

            is ArticlesListIntent.ArticleClicked ->
                _effects.send(ArticlesListEffect.OpenArticle(intent.articleId))

            is ArticlesListIntent.SendAnalytics ->
                analytics.logEvent(intent.event, intent.properties)
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
