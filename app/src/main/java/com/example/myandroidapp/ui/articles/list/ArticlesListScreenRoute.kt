package com.example.myandroidapp.ui.articles.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArticlesListScreenRoute(
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticlesListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is ArticlesListSideEffect.NavigateToDetail ->
                    onNavigateToDetail(effect.articleId)
            }
        }
    }

    ArticlesListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
