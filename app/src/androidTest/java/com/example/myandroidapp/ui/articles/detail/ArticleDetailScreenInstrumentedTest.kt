package com.example.myandroidapp.ui.articles.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.myandroidapp.test.TestFixtures
import com.example.myandroidapp.ui.common.UiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticleDetailScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsLoadingState() {
        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Loading,
                ),
                actions = ArticleDetailActions(
                    onBack = {},
                ),
            )
        }

        // Loading state shows a Lottie animation; we verify the top bar is present
        composeTestRule.onNodeWithText("Article Details").assertIsDisplayed()
    }

    @Test
    fun showsErrorState() {
        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Error("Failed to load article"),
                ),
                actions = ArticleDetailActions(
                    onBack = {},
                ),
            )
        }

        composeTestRule.onNodeWithText("Failed to load article").assertIsDisplayed()
    }

    @Test
    fun showsArticleDetailOnSuccess() {
        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Success(
                        ArticleDetailState(article = TestFixtures.articleDetail)
                    ),
                ),
                actions = ArticleDetailActions(
                    onBack = {},
                ),
            )
        }

        composeTestRule.onNodeWithText("Detailed Article Title").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "This is a comprehensive summary of the article with all details included."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("ESA").assertIsDisplayed()
    }

    @Test
    fun backButtonCallsOnBack() {
        var backCalled = false

        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Loading,
                ),
                actions = ArticleDetailActions(
                    onBack = { backCalled = true },
                ),
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backCalled)
    }

    @Test
    fun showsPublishedDateOnSuccess() {
        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Success(
                        ArticleDetailState(article = TestFixtures.articleDetail)
                    ),
                ),
                actions = ArticleDetailActions(
                    onBack = {},
                ),
            )
        }

        composeTestRule.onNodeWithText("Published: 2026-05-13").assertIsDisplayed()
    }
}
