package com.example.myandroidapp.ui.articles.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.paging.PagingData
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.test.TestFixtures
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticlesListScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsEmptyStateWhenNoArticles() {
        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "",
                    articles = flowOf(PagingData.empty()),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No articles available").assertIsDisplayed()
    }

    @Test
    fun showsArticlesInList() {
        val pagingData = PagingData.from(TestFixtures.articles)

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "",
                    articles = flowOf(pagingData),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SpaceX Launches Starship").assertIsDisplayed()
        composeTestRule.onNodeWithText("NASA Mars Rover Update").assertIsDisplayed()
    }

    @Test
    fun searchBarUpdatesQuery() {
        var query by mutableStateOf("")

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = query,
                    articles = flowOf(PagingData.empty()),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = { query = it },
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.onNodeWithText("Search articles\u2026").performTextInput("mars")

        assertEquals("mars", query)
    }

    @Test
    fun clearSearchButtonResetsQuery() {
        var query by mutableStateOf("nasa")

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = query,
                    articles = flowOf(PagingData.empty()),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = { query = it },
                    onClearSearch = { query = "" },
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.onNodeWithContentDescription("Clear search").performClick()

        assertEquals("", query)
    }

    @Test
    fun clickingArticleCallsOnArticleClick() {
        val clickedIds = mutableListOf<Int>()
        val pagingData = PagingData.from(TestFixtures.articles)

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "",
                    articles = flowOf(pagingData),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = { clickedIds.add(it) },
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SpaceX Launches Starship").performClick()

        assertEquals(1, clickedIds.size)
        assertEquals(1, clickedIds.first())
    }

    @Test
    fun showsNoResultsWhenSearchHasNoMatches() {
        val pagingData = PagingData.empty<Article>()

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "nonexistent",
                    articles = flowOf(pagingData),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No results found").assertIsDisplayed()
    }

    @Test
    fun showsCorrectNewsSiteOnArticleCard() {
        val pagingData = PagingData.from(TestFixtures.articles)

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "",
                    articles = flowOf(pagingData),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Space.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("NASA").assertIsDisplayed()
    }
}
