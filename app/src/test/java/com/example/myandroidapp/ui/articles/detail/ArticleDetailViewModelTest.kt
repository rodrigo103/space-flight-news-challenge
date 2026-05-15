package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import com.example.myandroidapp.data.Article
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.test.MainDispatcherRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ArticleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_initiallyLoading() = runTest {
        val viewModel = ArticleDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
            repository = FakeDetailRepository(),
        )
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun loadArticle_onSuccess_populatesDetail() = runTest {
        val repository = FakeDetailRepository()
        val viewModel = ArticleDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
            repository = repository,
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.article)
        assertEquals("Article Detail", viewModel.uiState.value.article?.title)
    }

    @Test
    fun clearError_clearsErrorMessage() = runTest {
        val viewModel = ArticleDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
            repository = FakeDetailRepository(),
        )
        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.error)
    }
}

private class FakeDetailRepository : ArticlesRepository {
    override suspend fun getArticles(limit: Int, offset: Int): Result<List<Article>> {
        return Result.success(emptyList())
    }

    override suspend fun searchArticles(query: String, limit: Int): Result<List<Article>> {
        return Result.success(emptyList())
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
