package com.example.myandroidapp.ui.articles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.Article
import com.example.myandroidapp.data.ArticlesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticlesListUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val offset: Int = 0,
    val hasMore: Boolean = true,
)

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticlesListUiState())
    val uiState: StateFlow<ArticlesListUiState> = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentState = _uiState.value
            repository.getArticles(offset = currentState.offset)
                .onSuccess { articles ->
                    _uiState.update {
                        it.copy(
                            articles = it.articles + articles,
                            isLoading = false,
                            offset = it.offset + articles.size,
                            hasMore = articles.size == PAGE_SIZE,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchArticles(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            repository.searchArticles(query)
                .onSuccess { articles ->
                    _uiState.update {
                        it.copy(
                            articles = articles,
                            isSearching = false,
                            offset = 0,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSearching = false, error = e.message) }
                }
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(searchQuery = "", articles = emptyList(), offset = 0, hasMore = true)
        }
        loadArticles()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
