package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
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

class ArticleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadArticle on success populates detail`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Article Detail", state.article?.title)
    }

    @Test
    fun `loadArticle on failure sets error`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticle(1) } returns Result.failure(Exception("Not found"))

        val viewModel = ArticleDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.article)
        assertEquals("Not found", state.error)
    }

    @Test
    fun `clearError clears error message`() = runTest {
        val repository = mockk<ArticlesRepository>()
        coEvery { repository.getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `throws when articleId missing from savedStateHandle`() = runTest {
        val repository = mockk<ArticlesRepository>()

        val exception = org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            ArticleDetailViewModel(
                repository = repository,
                savedStateHandle = SavedStateHandle(),
            )
        }
        assertTrue(exception.message?.contains("articleId") == true)
    }
}
