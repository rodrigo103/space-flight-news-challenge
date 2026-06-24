package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleDetailPaneViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ArticleDetailState>>(UiState.Loading)
    val state: StateFlow<UiState<ArticleDetailState>> = _state.asStateFlow()

    private val _sideEffects = Channel<ArticleDetailSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ArticleDetailSideEffect> = _sideEffects.receiveAsFlow()

    private var currentArticleId: Int? = null

    fun onEvent(event: ArticleDetailEvent) {
        when (event) {
            ArticleDetailEvent.Retry -> currentArticleId?.let { loadArticle(it) }
            ArticleDetailEvent.Back -> {
                viewModelScope.launch {
                    _sideEffects.send(ArticleDetailSideEffect.NavigateBack)
                }
            }
        }
    }

    fun loadArticle(articleId: Int) {
        currentArticleId = articleId
        analytics.logScreenView("ArticleDetail_$articleId")
        viewModelScope.launch {
            _state.value = UiState.Loading
            getArticle(articleId)
                .onSuccess { article ->
                    analytics.logEvent(
                        "article_loaded",
                        mapOf("id" to article.id.toString()),
                    )
                    _state.value = UiState.Success(
                        ArticleDetailState(article = article),
                    )
                }
                .onFailure { e ->
                    analytics.logError(e, "loadArticle_$articleId")
                    _state.value = UiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
