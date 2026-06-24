package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleDetailState(
    val article: Article,
)

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val articleId: Int =
        checkNotNull(savedStateHandle["articleId"]) { "articleId required" }

    private val _uiState = MutableStateFlow<UiState<ArticleDetailState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ArticleDetailState>> = _uiState.asStateFlow()

    private val _sideEffects = Channel<ArticleDetailSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ArticleDetailSideEffect> = _sideEffects.receiveAsFlow()

    init {
        analytics.logScreenView("ArticleDetail_$articleId")
        loadArticle()
    }

    fun loadArticle() {
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            getArticle(articleId)
                .onSuccess { article ->
                    analytics.logEvent(
                        "article_loaded",
                        mapOf("id" to article.id.toString()),
                    )
                    _uiState.value = UiState.Success(
                        ArticleDetailState(article = article),
                    )
                }
                .onFailure { e ->
                    analytics.logError(e, "loadArticle_$articleId")
                    _uiState.value = UiState.Error(e.message ?: "Unknown error")
                }
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _sideEffects.send(ArticleDetailSideEffect.NavigateBack)
        }
    }
}
