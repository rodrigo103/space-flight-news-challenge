package com.example.myandroidapp.ui.articles.detail

import com.example.myandroidapp.ui.common.UiState

sealed interface ArticleDetailEvent {
    data object Retry : ArticleDetailEvent
    data object Back : ArticleDetailEvent
}

sealed interface ArticleDetailSideEffect {
    data object NavigateBack : ArticleDetailSideEffect
}
