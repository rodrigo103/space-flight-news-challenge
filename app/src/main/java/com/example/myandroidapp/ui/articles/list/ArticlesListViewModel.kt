package com.example.myandroidapp.ui.articles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.Article
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.data.preferences.AppPreferences
import com.example.myandroidapp.ui.UiState
import com.example.myandroidapp.analytics.AnalyticsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class ArticlesListState(
    val articles: List<Article> = emptyList(),
    val searchQuery: String = "",
    val offset: Int = 0,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isSearching: Boolean = false,
)

sealed class ArticlesListEvent {
    data class Error(val message: String) : ArticlesListEvent()
}

data class ArticlesListActions(
    val onArticleClick: (Int) -> Unit,
    val onSearch: (String) -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onLoadMore: () -> Unit,
    val onRetry: () -> Unit,
)

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    private val analytics: AnalyticsHelper,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ArticlesListState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ArticlesListState>> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ArticlesListEvent>()
    val events: SharedFlow<ArticlesListEvent> = _events.asSharedFlow()

    private val _selectedArticleId = MutableStateFlow<Int?>(null)
    val selectedArticleId: StateFlow<Int?> = _selectedArticleId.asStateFlow()

    fun onArticleSelected(id: Int) {
        _selectedArticleId.value = id
        analytics.logEvent("article_selected", mapOf("id" to id.toString()))
    }

    init {
        analytics.logScreenView("ArticlesList")
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch {
            val currentState = (_uiState.value as? UiState.Success)?.data
            if (currentState != null) {
                _uiState.update { UiState.Success(currentState.copy(isLoadingMore = true)) }
            } else {
                _uiState.value = UiState.Loading
            }
            val offset = currentState?.offset ?: 0
            val result = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                repository.getArticles(offset = offset)
            }
            if (result == null) {
                val existing = (_uiState.value as? UiState.Success)?.data
                if (existing != null) {
                    _uiState.value = UiState.Success(existing.copy(isLoadingMore = false))
                    _events.emit(ArticlesListEvent.Error("Request timed out"))
                } else {
                    _uiState.value = UiState.Error("Request timed out")
                }
                return@launch
            }
            result
                .onSuccess { articles ->
                    val existing = (_uiState.value as? UiState.Success)?.data
                    val merged = if (existing != null) {
                        existing.copy(
                            articles = existing.articles + articles,
                            offset = existing.offset + articles.size,
                            hasMore = articles.size == PAGE_SIZE,
                            isLoadingMore = false,
                        )
                    } else {
                        ArticlesListState(
                            articles = articles,
                            offset = articles.size,
                            hasMore = articles.size == PAGE_SIZE,
                        )
                    }
                    analytics.logEvent("articles_loaded", mapOf("count" to articles.size.toString()))
                    _uiState.value = UiState.Success(merged)
                }
                .onFailure { e ->
                    analytics.logError(e, "loadArticles")
                    val existing = (_uiState.value as? UiState.Success)?.data
                    if (existing != null) {
                        _uiState.value = UiState.Success(existing.copy(isLoadingMore = false))
                        _events.emit(ArticlesListEvent.Error(e.message ?: "Unknown error"))
                    } else {
                        _uiState.value = UiState.Error(e.message ?: "Unknown error")
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(current.copy(searchQuery = query))
    }

    fun searchArticles(query: String) {
        viewModelScope.launch {
            val current = (_uiState.value as? UiState.Success)?.data
                ?: ArticlesListState()
            _uiState.value = UiState.Success(current.copy(isSearching = true))
            val result = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                repository.searchArticles(query)
            }
            if (result == null) {
                _uiState.value = UiState.Success(current.copy(isSearching = false))
                _events.emit(ArticlesListEvent.Error("Request timed out"))
                return@launch
            }
            result
                .onSuccess { articles ->
                    analytics.logEvent("search_completed", mapOf("query" to query, "count" to articles.size.toString()))
                    _uiState.value = UiState.Success(
                        current.copy(
                            articles = articles,
                            isSearching = false,
                            offset = 0,
                        )
                    )
                }
                .onFailure { e ->
                    _uiState.value = UiState.Success(current.copy(isSearching = false))
                    _events.emit(ArticlesListEvent.Error(e.message ?: "Search failed"))
                }
        }
    }

    fun clearSearch() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(
            current.copy(searchQuery = "", articles = emptyList(), offset = 0, hasMore = true)
        )
        loadArticles()
    }

    fun retry() {
        loadArticles()
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val REQUEST_TIMEOUT_MS = 30_000L
    }
}
