package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.test.MainDispatcherRule
import com.example.myandroidapp.ui.UiState
import io.mockk.coEvery
import io.mockk.mockk
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
    fun `loadArticle on success populates detail`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            repository = repository,
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
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticle(1) } returns Result.failure(Exception("Not found"))

        val viewModel = ArticleDetailViewModel(
            repository = repository,
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
        val repository = mockk<ArticlesRepository>()

        val exception = assertThrows(IllegalStateException::class.java) {
            ArticleDetailViewModel(
                repository = repository,
                analytics = analytics,
                savedStateHandle = SavedStateHandle(),
            )
        }
        assertTrue(exception.message?.contains("articleId") == true)
    }
}
