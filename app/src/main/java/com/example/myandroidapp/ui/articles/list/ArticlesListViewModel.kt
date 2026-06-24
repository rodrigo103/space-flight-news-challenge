package com.example.myandroidapp.ui.articles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.repository.ArticlesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(ArticlesListState())
    val state: StateFlow<ArticlesListState> = _state.asStateFlow()

    private val _sideEffects = Channel<ArticlesListSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ArticlesListSideEffect> = _sideEffects.receiveAsFlow()

    init {
        analytics.logScreenView("ArticlesList")
        observeArticles()
    }

    fun onEvent(event: ArticlesListEvent) {
        when (event) {
            is ArticlesListEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
            }

            is ArticlesListEvent.ClearSearch -> {
                _state.update { it.copy(searchQuery = "") }
            }

            is ArticlesListEvent.ArticleClicked -> {
                analytics.logEvent(
                    "article_selected",
                    mapOf("id" to event.articleId.toString()),
                )
                viewModelScope.launch {
                    _sideEffects.send(
                        ArticlesListSideEffect.NavigateToDetail(event.articleId),
                    )
                }
            }
        }
    }

    private fun observeArticles() {
        viewModelScope.launch {
            _state
                .map { it.searchQuery }
                .distinctUntilChanged()
                .debounce(300)
                .collect { query ->
                    _state.update {
                        it.copy(
                            articles = repository.getArticlesPaged(
                                searchQuery = query.ifBlank { null },
                            ),
                        )
                    }
                }
        }
    }
}
