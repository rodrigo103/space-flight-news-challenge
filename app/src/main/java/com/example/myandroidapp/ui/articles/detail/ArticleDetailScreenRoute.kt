package com.example.myandroidapp.ui.articles.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.ui.common.UiState

@Composable
fun ArticleDetailScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if ((state as? UiState.Success)?.data?.navigateBack == true) {
            onBack()
        }
    }

    ArticleDetailScreen(
        attributes = ArticleDetailAttributes(state = state),
        actions = ArticleDetailActions(
            onBack = onBack,
            onRetry = viewModel::loadArticle,
            onDelete = viewModel::onDeleteArticle,
        ),
        modifier = modifier,
    )
}
