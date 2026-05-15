package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.Article
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.data.DefaultArticlesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArticleDetailUiState(
    val article: Article? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ArticleDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ArticlesRepository = DefaultArticlesRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    private val articleId: Int = checkNotNull(savedStateHandle.get<Int>("articleId"))

    init {
        loadArticle()
    }

    fun loadArticle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getArticle(articleId)
                .onSuccess { article ->
                    _uiState.update { it.copy(article = article, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
