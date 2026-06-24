package com.example.myandroidapp.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.ui.articles.list.ArticleCard
import com.example.myandroidapp.ui.articles.list.ArticlesListScreen
import com.example.myandroidapp.ui.articles.list.ArticlesListState
import kotlinx.coroutines.flow.flowOf

private val sampleArticles = listOf(
    Article(
        id = 1,
        title = "SpaceX Successfully Launches Starship to Mars",
        summary = "In a historic milestone, SpaceX's Starship completed its first crewed mission to the Red Planet.",
        imageUrl = "https://picsum.photos/id/1/400/200",
        newsSite = "Space News",
        publishedAt = "2026-05-15",
        url = "https://example.com",
        authors = emptyList(),
    ),
    Article(
        id = 2,
        title = "NASA's Artemis Program Achieves New Milestone with Lunar Base Construction",
        summary = "NASA has announced the successful completion of the first phase of lunar base construction.",
        imageUrl = "https://picsum.photos/id/2/400/200",
        newsSite = "NASA",
        publishedAt = "2026-05-14",
        url = "https://example.com",
        authors = emptyList(),
    ),
    Article(
        id = 3,
        title = "James Webb Telescope Discovers New Exoplanet with Clear Signs of Water Vapor",
        summary = "The James Webb Space Telescope has identified a distant exoplanet with an atmosphere containing significant amounts of water vapor.",
        imageUrl = "https://picsum.photos/id/3/400/200",
        newsSite = "Space Telescope",
        publishedAt = "2026-05-13",
        url = "https://example.com",
        authors = emptyList(),
    ),
)

@Preview(showBackground = true)
@Composable
private fun ArticlesListScreenLoadingPreview() {
    MaterialTheme {
        ArticlesListScreen(
            state = ArticlesListState(
                searchQuery = "",
                articles = null,
            ),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ArticleCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ArticleCard(
                article = sampleArticles.first(),
                onClick = {},
            )
        }
    }
}

@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ArticleCardFullScreenPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ArticleCard(article = sampleArticles[0], onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            ArticleCard(article = sampleArticles[1], onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            ArticleCard(article = sampleArticles[2], onClick = {})
        }
    }
}
