package com.example.myandroidapp.ui.articles.detail

import com.example.myandroidapp.ui.common.UiState

data class ArticleDetailAttributes(
    val state: UiState<ArticleDetailState>,
)

data class ArticleDetailActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit = {},
)
