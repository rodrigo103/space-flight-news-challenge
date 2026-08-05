package com.example.myandroidapp.ui.articles.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArticlesListScreenRoute(
    onArticleClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticlesListViewModel = koinViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    ArticlesListScreen(
        attributes = ArticlesListAttributes(
            searchQuery = searchQuery,
            articles = viewModel.articles,
        ),
        actions = ArticlesListActions(
            onSearchTextChange = viewModel::onSearchTextChange,
            onClearSearch = viewModel::clearSearch,
            onArticleClick = onArticleClick,
            sendAnalytics = viewModel::sendAnalytics,
        ),
        modifier = modifier,
    )
}
