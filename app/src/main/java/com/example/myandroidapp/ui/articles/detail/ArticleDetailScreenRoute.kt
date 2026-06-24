package com.example.myandroidapp.ui.articles.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArticleDetailScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                ArticleDetailSideEffect.NavigateBack -> onBack()
            }
        }
    }

    ArticleDetailScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
