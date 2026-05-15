package com.example.myandroidapp.ui.articles.list

import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticlesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadArticles on success populates list and stops loading`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(
            listOf(TestArticleData.article1, TestArticleData.article2)
        )

        val viewModel = ArticlesListViewModel(repository)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.articles.size)
        assertEquals("Article 1", state.articles[0].title)
    }

    @Test
    fun `loadArticles on failure sets error`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.failure(
            Exception("Network error")
        )

        val viewModel = ArticlesListViewModel(repository)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.articles.isEmpty())
        assertEquals("Network error", state.error)
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

        val viewModel = ArticlesListViewModel(repository)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.articles.size)

        viewModel.loadArticles()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.articles.size)
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

        val viewModel = ArticlesListViewModel(repository)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.articles.size)

        viewModel.searchArticles("nasa")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.articles.size)
    }

    @Test
    fun `searchArticles on failure sets error`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(emptyList())
        coEvery { repository.searchArticles("nasa") } returns Result.failure(
            Exception("Search failed")
        )

        val viewModel = ArticlesListViewModel(repository)
        viewModel.searchArticles("nasa")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Search failed", viewModel.uiState.value.error)
    }

    @Test
    fun `onSearchQueryChanged updates query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(emptyList())

        val viewModel = ArticlesListViewModel(repository)
        viewModel.onSearchQueryChanged("nasa")
        assertEquals("nasa", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `clearSearch resets query and reloads`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(
            listOf(TestArticleData.article1)
        )

        val viewModel = ArticlesListViewModel(repository)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.articles.size)

        viewModel.onSearchQueryChanged("nasa")
        viewModel.clearSearch()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `clearError clears error message`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticles(offset = 0) } returns Result.success(emptyList())
        coEvery { repository.searchArticles(any()) } returns Result.failure(
            Exception("Not found")
        )

        val viewModel = ArticlesListViewModel(repository)
        viewModel.searchArticles("fail")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}
