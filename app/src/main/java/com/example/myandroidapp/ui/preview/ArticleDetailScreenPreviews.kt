package com.example.myandroidapp.ui.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.ui.common.UiState
import com.example.myandroidapp.ui.articles.detail.ArticleDetailActions
import com.example.myandroidapp.ui.articles.detail.ArticleDetailAttributes
import com.example.myandroidapp.ui.articles.detail.ArticleDetailScreen
import com.example.myandroidapp.ui.articles.detail.ArticleDetailState

private val detailArticle = Article(
    id = 1,
    title = "SpaceX Successfully Launches Starship to Mars",
    summary = "In a historic milestone, SpaceX's Starship completed its first crewed mission to the Red Planet, carrying a team of 12 astronauts. The mission marks the beginning of a new era in interplanetary travel.",
    imageUrl = "https://picsum.photos/id/1/800/400",
    newsSite = "Space News",
    publishedAt = "2026-05-15",
    url = "https://example.com",
    authors = emptyList(),
)

private val dummyActions = ArticleDetailActions(
    onBack = {},
)

// --- States ---

@Preview(showBackground = true)
@Composable
private fun ArticleDetailScreenLoadingPreview() {
    MaterialTheme {
        ArticleDetailScreen(
            attributes = ArticleDetailAttributes(
                state = UiState.Loading,
            ),
            actions = dummyActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleDetailScreenSuccessPreview() {
    MaterialTheme {
        ArticleDetailScreen(
            attributes = ArticleDetailAttributes(
                state = UiState.Success(ArticleDetailState(article = detailArticle)),
            ),
            actions = dummyActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleDetailScreenErrorPreview() {
    MaterialTheme {
        ArticleDetailScreen(
            attributes = ArticleDetailAttributes(
                state = UiState.Error("Unable to load article. Check your connection."),
            ),
            actions = dummyActions,
        )
    }
}
