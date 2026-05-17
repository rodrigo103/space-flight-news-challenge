package com.example.myandroidapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myandroidapp.data.Article
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.ui.articles.detail.articleDetailContentSettings
import com.example.myandroidapp.ui.articles.list.ArticlesListScreen
import com.example.myandroidapp.ui.articles.list.ArticlesListViewModel

@Composable
fun DualPaneScreen(
    repository: ArticlesRepository,
    modifier: Modifier = Modifier,
) {
    val listViewModel: ArticlesListViewModel = hiltViewModel()

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(0.4f).fillMaxSize()) {
                ArticlesListScreen(
                    onArticleClick = { articleId ->
                        listViewModel.onArticleSelected(articleId)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            Box(modifier = Modifier.weight(0.6f).fillMaxSize()) {
                val selectedArticleId by listViewModel.selectedArticleId.collectAsStateWithLifecycle()
                DetailPane(
                    articleId = selectedArticleId,
                    repository = repository,
                )
            }
        }
    }
}

@Composable
private fun DetailPane(
    articleId: Int?,
    repository: ArticlesRepository,
) {
    if (articleId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Select an article",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        var detailState by remember(articleId) { mutableStateOf<UiState<Article>>(UiState.Loading) }

        LaunchedEffect(articleId) {
            detailState = UiState.Loading
            repository.getArticle(articleId)
                .onSuccess { article -> detailState = UiState.Success(article) }
                .onFailure { e -> detailState = UiState.Error(e.message ?: "Unknown error") }
        }

        when (val state = detailState) {
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
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            is UiState.Success -> articleDetailContentSettings(
                article = state.data,
            )()
        }
    }
}
