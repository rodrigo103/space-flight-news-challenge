package com.example.myandroidapp.ui.articles.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Route MVI de la lista: observa el `StateFlow` de estado, traduce las acciones de la pantalla a
 * [ArticlesListIntent]s y consume los efectos one-shot ([ArticlesListEffect]).
 */
@Composable
fun ArticlesListScreenRoute(
    onArticleClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticlesListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ArticlesListEffect.OpenArticle -> onArticleClick(effect.articleId)
            }
        }
    }

    ArticlesListScreen(
        attributes = ArticlesListAttributes(
            searchQuery = state.searchQuery,
            articles = viewModel.articles,
        ),
        actions = ArticlesListActions(
            onSearchTextChange = { viewModel.onIntent(ArticlesListIntent.SearchQueryChanged(it)) },
            onClearSearch = { viewModel.onIntent(ArticlesListIntent.ClearSearch) },
            onArticleClick = { viewModel.onIntent(ArticlesListIntent.ArticleClicked(it)) },
            sendAnalytics = { event, properties ->
                viewModel.onIntent(ArticlesListIntent.SendAnalytics(event, properties))
            },
        ),
        modifier = modifier,
    )
}
