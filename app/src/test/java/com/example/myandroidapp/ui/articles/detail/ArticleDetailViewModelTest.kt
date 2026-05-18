package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.data.preferences.AppPreferences
import com.example.myandroidapp.test.MainDispatcherRule
import com.example.myandroidapp.ui.UiState
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val preferences = mockk<AppPreferences>(relaxed = true)

    @Test
    fun `initial state is Loading`() {
        val repository = mockk<ArticlesRepository>()

        val viewModel = ArticleDetailViewModel(
            repository = repository,
            analytics = analytics,
            preferences = preferences,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )

        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    @Test
    fun `null error message falls back to Unknown error`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticle(1) } returns Result.failure(Exception())

        val viewModel = ArticleDetailViewModel(
            repository = repository,
            analytics = analytics,
            preferences = preferences,
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
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            repository = repository,
            analytics = analytics,
            preferences = preferences,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { analytics.logEvent("article_loaded", mapOf("id" to "1")) }
    }

    @Test
    fun `persists last opened article id on success`() = runTest {
        val preferences = mockk<AppPreferences>(relaxed = true)
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            repository = repository,
            analytics = analytics,
            preferences = preferences,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferences.setLastOpenedArticleId(1) }
    }

    @Test
    fun `loadArticle on success populates detail`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            repository = repository,
            analytics = analytics,
            preferences = preferences,
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
            preferences = preferences,
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
                preferences = preferences,
                savedStateHandle = SavedStateHandle(),
            )
        }
        assertTrue(exception.message?.contains("articleId") == true)
    }
}
