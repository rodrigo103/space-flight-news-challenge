package com.example.myandroidapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.R
import com.example.myandroidapp.ui.articles.detail.ArticleDetailEvent
import com.example.myandroidapp.ui.articles.detail.ArticleDetailPaneViewModel
import com.example.myandroidapp.ui.articles.detail.articleDetailContentSettings
import com.example.myandroidapp.ui.articles.list.ArticlesListEvent
import com.example.myandroidapp.ui.articles.list.ArticlesListScreen
import com.example.myandroidapp.ui.articles.list.ArticlesListViewModel
import com.example.myandroidapp.ui.common.UiState

@Composable
fun DualPaneScreen(
    selectedArticleId: Int?,
    onArticleSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    listViewModel: ArticlesListViewModel = hiltViewModel(),
    detailViewModel: ArticleDetailPaneViewModel = hiltViewModel(),
) {
    val listState by listViewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .weight(LIST_WEIGHT)
                    .fillMaxSize(),
            ) {
                ArticlesListScreen(
                    state = listState,
                    onEvent = { event ->
                        when (event) {
                            is ArticlesListEvent.ArticleClicked ->
                                onArticleSelected(event.articleId)
                            else -> listViewModel.onEvent(event)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            Box(
                modifier = Modifier
                    .weight(DETAIL_WEIGHT)
                    .fillMaxSize(),
            ) {
                DetailPane(
                    articleId = selectedArticleId,
                    viewModel = detailViewModel,
                )
            }
        }
    }
}

@Composable
private fun DetailPane(
    articleId: Int?,
    viewModel: ArticleDetailPaneViewModel,
) {
    if (articleId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.select_an_article),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LaunchedEffect(articleId) {
            viewModel.loadArticle(articleId)
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        when (val s = state) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is UiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(
                        onClick = { viewModel.onEvent(ArticleDetailEvent.Retry) },
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            is UiState.Success -> articleDetailContentSettings(
                article = s.data.article,
            )()
        }
    }
}

private const val LIST_WEIGHT = 0.4f
private const val DETAIL_WEIGHT = 0.6f
