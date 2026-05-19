package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleDetailPaneViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ArticleDetailState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ArticleDetailState>> = _uiState.asStateFlow()

    fun loadArticle(articleId: Int) {
        analytics.logScreenView("ArticleDetail_$articleId")
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            getArticle(articleId)
                .onSuccess { article ->
                    analytics.logEvent("article_loaded", mapOf("id" to article.id.toString()))
                    _uiState.value = UiState.Success(ArticleDetailState(article = article))
                }
                .onFailure { e ->
                    analytics.logError(e, "loadArticle_$articleId")
                    _uiState.value = UiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
