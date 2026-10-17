package com.example.myandroidapp.ui.articles.detail

import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.test.MainDispatcherRule
import com.example.myandroidapp.test.TestArticleData
import com.example.myandroidapp.ui.common.UiState
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticleDetailPaneViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    @Test
    fun `initial state is Loading`() {
        val viewModel = ArticleDetailPaneViewModel(mockk(relaxed = true), analytics)

        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    @Test
    fun `loadArticle success emits Success with the article`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Success(TestArticleData.articleDetail)
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(TestArticleData.articleDetail, (state as UiState.Success).data.article)
        verify { analytics.logScreenView("ArticleDetail_1") }
        verify { analytics.logEvent("article_loaded", mapOf("id" to "1")) }
    }

    @Test
    fun `loadArticle failure emits Error with the message`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Failure(Exception("boom"))
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("boom", (state as UiState.Error).message)
        verify { analytics.logError(any(), "loadArticle_1") }
    }

    @Test
    fun `loadArticle failure without message falls back to Unknown error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Failure(Exception())
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown error", (viewModel.uiState.value as UiState.Error).message)
    }

    @Test
    fun `loadArticle keeps Loading until the request resolves`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Success(TestArticleData.articleDetail)
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        assertTrue(viewModel.uiState.value is UiState.Loading)

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)
    }
}
