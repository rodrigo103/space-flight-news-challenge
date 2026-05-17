package com.example.myandroidapp.ui.articles.list

import app.cash.turbine.test
import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.test.MainDispatcherRule
import com.example.myandroidapp.ui.UiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticlesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    @Test
    fun `loadArticles on success populates list and stops loading`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(
            listOf(TestArticleData.article1, TestArticleData.article2)
        )

        val viewModel = ArticlesListViewModel(repository, analytics)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(2, data.articles.size)
        assertEquals("Article 1", data.articles[0].title)
    }

    @Test
    fun `loadArticles on failure sets error`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.failure(
            Exception("Network error")
        )

        val viewModel = ArticlesListViewModel(repository, analytics)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Network error", (state as UiState.Error).message)
    }

    @Test
    fun `loadArticles appends to existing list on pagination`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(
            listOf(TestArticleData.article1)
        )
        coEvery { repository.getArticles(offset = 1) } returns Result.success(
            listOf(TestArticleData.article2)
        )

        val viewModel = ArticlesListViewModel(repository, analytics)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        var data = (viewModel.uiState.value as UiState.Success).data
        assertEquals(1, data.articles.size)

        viewModel.loadArticles()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        data = (viewModel.uiState.value as UiState.Success).data
        assertEquals(2, data.articles.size)
    }

    @Test
    fun `searchArticles on success replaces list`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(
            listOf(TestArticleData.article1, TestArticleData.article2)
        )
        coEvery { repository.searchArticles("nasa") } returns Result.success(
            listOf(TestArticleData.article1)
        )

        val viewModel = ArticlesListViewModel(repository, analytics)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        var data = (viewModel.uiState.value as UiState.Success).data
        assertEquals(2, data.articles.size)

        viewModel.searchArticles("nasa")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        data = (viewModel.uiState.value as UiState.Success).data
        assertEquals(1, data.articles.size)
    }

    @Test
    fun `searchArticles on failure emits error event`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(emptyList())
        coEvery { repository.searchArticles("nasa") } returns Result.failure(
            Exception("Search failed")
        )

        val viewModel = ArticlesListViewModel(repository, analytics)
        viewModel.events.test {
            viewModel.searchArticles("nasa")
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ArticlesListEvent.Error)
            assertEquals("Search failed", (event as ArticlesListEvent.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchQueryChanged updates query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(emptyList())

        val viewModel = ArticlesListViewModel(repository, analytics)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("nasa")
        val data = (viewModel.uiState.value as UiState.Success).data
        assertEquals("nasa", data.searchQuery)
    }

    @Test
    fun `clearSearch resets query and reloads`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(
            listOf(TestArticleData.article1)
        )
        coEvery { repository.getArticles(offset = 0) } returns Result.success(
            listOf(TestArticleData.article1)
        )

        val viewModel = ArticlesListViewModel(repository, analytics)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        var data = (viewModel.uiState.value as UiState.Success).data
        assertEquals(1, data.articles.size)

        viewModel.onSearchQueryChanged("nasa")
        viewModel.clearSearch()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        data = (viewModel.uiState.value as UiState.Success).data
        assertEquals("", data.searchQuery)
    }
}
