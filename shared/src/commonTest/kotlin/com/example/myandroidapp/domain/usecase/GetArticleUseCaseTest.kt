package com.example.myandroidapp.domain.usecase

import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.test.FakeArticlesRepository
import com.example.myandroidapp.test.TestArticleData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetArticleUseCaseTest {

    private val repository = FakeArticlesRepository()
    private val useCase = GetArticleUseCase(repository)

    @Test
    fun `invoke returns article on network success`() = runTest {
        val article = TestArticleData.articleDetail
        repository.articleResult = ApiResult.Success(article)

        val result = useCase(1)

        assertEquals(article, assertIs<ApiResult.Success<*>>(result).data)
    }

    @Test
    fun `invoke returns article from cache on network failure`() = runTest {
        val cached = TestArticleData.article1
        repository.articleResult = ApiResult.Failure(IllegalStateException("Network error"))
        repository.cachedArticle = cached

        val result = useCase(1)

        assertEquals(cached, assertIs<ApiResult.Success<*>>(result).data)
    }

    @Test
    fun `invoke returns failure when network fails and no cache`() = runTest {
        repository.articleResult = ApiResult.Failure(IllegalStateException("Not found"))
        repository.cachedArticle = null

        val result = useCase(1)

        assertEquals("Not found", assertIs<ApiResult.Failure>(result).error.message)
    }

    @Test
    fun `invoke falls back to cache when network times out`() = runTest {
        val cached = TestArticleData.article2
        repository.getArticleDelayMs = 60_000
        repository.cachedArticle = cached

        val result = useCase(2)

        assertEquals(cached, assertIs<ApiResult.Success<*>>(result).data)
    }

    @Test
    fun `invoke returns timeout error when no network and no cache`() = runTest {
        repository.getArticleDelayMs = 60_000
        repository.cachedArticle = null

        val result = useCase(3)

        assertEquals("Request timed out", assertIs<ApiResult.Failure>(result).error.message)
    }

    @Test
    fun `invoke prefers network over cache when both available`() = runTest {
        repository.articleResult = ApiResult.Success(TestArticleData.articleDetail)
        repository.cachedArticle = TestArticleData.article1

        val result = useCase(1)

        assertEquals(TestArticleData.articleDetail, assertIs<ApiResult.Success<*>>(result).data)
    }
}
