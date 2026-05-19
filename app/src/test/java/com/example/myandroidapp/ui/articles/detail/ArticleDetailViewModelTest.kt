package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.test.MainDispatcherRule
import com.example.myandroidapp.ui.common.UiState
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    @Test
    fun `initial state is Loading`() {
        val getArticle = mockk<GetArticleUseCase>()

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )

        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    @Test
    fun `null error message falls back to Unknown error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception())

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Unknown error", (state as UiState.Error).message)
    }

    @Test
    fun `logs event on success`() = runTest {
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { analytics.logEvent("article_loaded", mapOf("id" to "1")) }
    }

    @Test
    fun `loadArticle on success populates detail on success`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("Article Detail", data.article.title)
    }

    @Test
    fun `loadArticle on failure sets error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception("Not found"))

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Not found", (state as UiState.Error).message)
    }

    @Test
    fun `throws when articleId missing from savedStateHandle`() {
        val getArticle = mockk<GetArticleUseCase>()

        val exception = assertThrows(IllegalStateException::class.java) {
            ArticleDetailViewModel(
                getArticle = getArticle,
                analytics = analytics,
                savedStateHandle = SavedStateHandle(),
            )
        }
        assertTrue(exception.message?.contains("articleId") == true)
    }
}
