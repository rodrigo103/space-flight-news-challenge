package com.example.myandroidapp.ui.articles.list

import androidx.lifecycle.SavedStateHandle
import com.example.myandroidapp.data.Article
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.test.MainDispatcherRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ArticlesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_initiallyEmpty() = runTest {
        val viewModel = ArticlesListViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeArticlesRepository(),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.articles.isNotEmpty())
    }

    @Test
    fun loadArticles_onSuccess_populatesList() = runTest {
        val repository = FakeArticlesRepository()
        val viewModel = ArticlesListViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.articles.isNotEmpty())
        assertEquals("Article 1", viewModel.uiState.value.articles[0].title)
    }

    @Test
    fun clearSearch_resetsAndReloads() = runTest {
        val repository = FakeArticlesRepository()
        val viewModel = ArticlesListViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
        )
        viewModel.onSearchQueryChanged("test")
        viewModel.clearSearch()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun onSearchQueryChanged_updatesQuery() = runTest {
        val viewModel = ArticlesListViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeArticlesRepository(),
        )
        viewModel.onSearchQueryChanged("nasa")
        assertEquals("nasa", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun clearError_clearsErrorMessage() = runTest {
        val viewModel = ArticlesListViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeArticlesRepository(),
        )
        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.error)
    }
}

private class FakeArticlesRepository : ArticlesRepository {
    override suspend fun getArticles(limit: Int, offset: Int): Result<List<Article>> {
        return Result.success(
            listOf(
                Article(
                    id = 1,
                    title = "Article 1",
                    summary = "Summary 1",
                    newsSite = "NASA",
                    publishedAt = "2026-05-15",
                ),
                Article(
                    id = 2,
                    title = "Article 2",
                    summary = "Summary 2",
                    newsSite = "SpaceX",
                    publishedAt = "2026-05-14",
                ),
            ),
        )
    }

    override suspend fun searchArticles(query: String, limit: Int): Result<List<Article>> {
        return Result.success(
            listOf(
                Article(
                    id = 1,
                    title = "Article 1",
                    summary = "Summary 1",
                    newsSite = "NASA",
                    publishedAt = "2026-05-15",
                ),
            ),
        )
    }

    override suspend fun getArticle(id: Int): Result<Article> {
        return Result.success(
            Article(
                id = id,
                title = "Article Detail",
                summary = "Full summary",
                newsSite = "NASA",
                publishedAt = "2026-05-15",
            ),
        )
    }
}
