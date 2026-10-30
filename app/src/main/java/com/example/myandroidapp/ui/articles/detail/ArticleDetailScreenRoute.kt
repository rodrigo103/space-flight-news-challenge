package com.example.myandroidapp.ui.articles.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.ui.common.UiState

/**
 * Route MVI del detalle: traduce el estado MVI al contrato visual de la pantalla, mapea acciones a
 * [ArticleDetailIntent]s y consume el efecto one-shot [ArticleDetailEffect.NavigateBack].
 */
@Composable
fun ArticleDetailScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ArticleDetailEffect.NavigateBack -> onBack()
            }
        }
    }

    val screenState: UiState<ArticleDetailState> = when (val current = state) {
        ArticleDetailUiState.Loading -> UiState.Loading
        is ArticleDetailUiState.Content ->
            UiState.Success(ArticleDetailState(article = current.article))

        is ArticleDetailUiState.Error -> UiState.Error(current.message)
    }

    ArticleDetailScreen(
        attributes = ArticleDetailAttributes(state = screenState),
        actions = ArticleDetailActions(
            onBack = onBack,
            onRetry = { viewModel.onIntent(ArticleDetailIntent.Retry) },
            onDelete = { viewModel.onIntent(ArticleDetailIntent.Delete) },
        ),
        modifier = modifier,
    )
}
