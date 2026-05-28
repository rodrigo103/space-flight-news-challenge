package com.example.myandroidapp.domain.usecase

import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetArticleUseCaseTest {

    private val repository = mockk<ArticlesRepository>()

    private fun createUseCase() = GetArticleUseCase(repository)

    @Test
    fun `invoke returns article on network success`() = runTest {
        val article = TestArticleData.articleDetail
        coEvery { repository.getArticle(1) } returns Result.success(article)

        val result = createUseCase()(1)

        assertTrue(result.isSuccess)
        assertEquals(article, result.getOrThrow())
    }

    @Test
    fun `invoke returns article from cache on network failure`() = runTest {
        val cached = TestArticleData.article1
        coEvery { repository.getArticle(1) } returns Result.failure(Exception("Network error"))
        coEvery { repository.getCachedArticle(1) } returns cached

        val result = createUseCase()(1)

        assertTrue(result.isSuccess)
        assertEquals(cached, result.getOrThrow())
    }

    @Test
    fun `invoke returns failure when network fails and no cache`() = runTest {
        coEvery { repository.getArticle(1) } returns Result.failure(Exception("Not found"))
        coEvery { repository.getCachedArticle(1) } returns null

        val result = createUseCase()(1)

        assertTrue(result.isFailure)
        assertEquals("Not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke falls back to cache when network times out`() = runTest {
        val cached = TestArticleData.article2
        coEvery { repository.getArticle(2) } coAnswers {
            delay(60_000)
            Result.success(mockk<Article>())
        }
        coEvery { repository.getCachedArticle(2) } returns cached

        val result = createUseCase()(2)

        assertTrue(result.isSuccess)
        assertEquals(cached, result.getOrThrow())
    }

    @Test
    fun `invoke returns timeout error when no network and no cache`() = runTest {
        coEvery { repository.getArticle(3) } coAnswers {
            delay(60_000)
            Result.success(mockk<Article>())
        }
        coEvery { repository.getCachedArticle(3) } returns null

        val result = createUseCase()(3)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message == "Request timed out")
    }

    @Test
    fun `invoke prefers network over cache when both available`() = runTest {
        val networkArticle = TestArticleData.articleDetail
        val cachedArticle = TestArticleData.article1
        coEvery { repository.getArticle(1) } returns Result.success(networkArticle)
        coEvery { repository.getCachedArticle(1) } returns cachedArticle

        val result = createUseCase()(1)

        assertTrue(result.isSuccess)
        assertEquals(networkArticle, result.getOrThrow())
    }
}
