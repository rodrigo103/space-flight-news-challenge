package com.example.myandroidapp.data

import com.example.myandroidapp.data.remote.ApiException
import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.data.repository.DefaultArticlesRepository
import com.example.myandroidapp.test.FakeArticleDao
import com.example.myandroidapp.test.RecordedRequests
import com.example.myandroidapp.test.TestJson
import com.example.myandroidapp.test.testApiService
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticlesRepositoryTest {

    private fun repository(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = TestJson.ARTICLE_RESPONSE,
        dao: FakeArticleDao = FakeArticleDao(),
        recorded: RecordedRequests? = null,
    ) = DefaultArticlesRepository(testApiService(status, body, recorded), dao)

    @Test
    fun `getArticles on 200 returns success`() = runTest {
        val result = repository().getArticles()

        val success = assertIs<ApiResult.Success<List<com.example.myandroidapp.domain.model.Article>>>(result)
        assertEquals(2, success.data.size)
        assertEquals("Article 1", success.data[0].title)
        assertEquals("Article 2", success.data[1].title)
    }

    @Test
    fun `getArticles on 500 returns failure with ServerError`() = runTest {
        val result = repository(
            status = HttpStatusCode.InternalServerError,
            body = """{"error":"Internal error"}""",
        ).getArticles()

        val failure = assertIs<ApiResult.Failure>(result)
        val error = assertIs<ApiException.ServerError>(failure.error)
        assertEquals(500, error.code)
    }

    @Test
    fun `getArticles on 404 returns failure with NotFound`() = runTest {
        val result = repository(
            status = HttpStatusCode.NotFound,
            body = """{"error":"Not found"}""",
        ).getArticles()

        val failure = assertIs<ApiResult.Failure>(result)
        assertIs<ApiException.NotFound>(failure.error)
    }

    @Test
    fun `getArticle on 200 returns success`() = runTest {
        val result = repository(body = TestJson.SINGLE_ARTICLE).getArticle(1)

        val success = assertIs<ApiResult.Success<com.example.myandroidapp.domain.model.Article>>(result)
        assertEquals("Article Detail", success.data.title)
    }

    @Test
    fun `getArticle on 404 returns failure`() = runTest {
        val result = repository(
            status = HttpStatusCode.NotFound,
            body = """{"error":"Not found"}""",
        ).getArticle(999)

        assertIs<ApiResult.Failure>(result)
    }

    @Test
    fun `getArticles on malformed JSON returns failure`() = runTest {
        val result = repository(body = """{broken json}""").getArticles()

        assertIs<ApiResult.Failure>(result)
    }

    @Test
    fun `searchArticles on 200 returns filtered results`() = runTest {
        val result = repository(body = TestJson.SEARCH_RESPONSE).searchArticles("Article 1")

        val success = assertIs<ApiResult.Success<List<com.example.myandroidapp.domain.model.Article>>>(result)
        assertEquals(1, success.data.size)
        assertEquals("Article 1", success.data[0].title)
    }

    @Test
    fun `searchArticles on 500 returns failure`() = runTest {
        val result = repository(
            status = HttpStatusCode.InternalServerError,
            body = """{"error":"Server error"}""",
        ).searchArticles("test")

        assertIs<ApiResult.Failure>(result)
    }

    @Test
    fun `searchArticles with empty results returns empty list`() = runTest {
        val result = repository(body = TestJson.EMPTY_RESPONSE).searchArticles("nonexistent")

        val success = assertIs<ApiResult.Success<List<com.example.myandroidapp.domain.model.Article>>>(result)
        assertTrue(success.data.isEmpty())
    }

    @Test
    fun `getArticles with custom limit and offset hits the right query`() = runTest {
        val recorded = RecordedRequests()

        repository(recorded = recorded).getArticles(limit = 10, offset = 5)

        val url = recorded.urls.single()
        assertContains(url, "limit=10")
        assertContains(url, "offset=5")
    }

    @Test
    fun `searchArticles sends the query as search param`() = runTest {
        val recorded = RecordedRequests()

        repository(body = TestJson.SEARCH_RESPONSE, recorded = recorded).searchArticles("mars")

        assertContains(recorded.urls.single(), "search=mars")
    }

    @Test
    fun `getCachedArticle returns the entity mapped to domain`() = runTest {
        val dao = FakeArticleDao()
        val repo = repository(dao = dao)
        dao.insert(
            com.example.myandroidapp.data.local.ArticleEntity(
                id = 7,
                title = "Cached",
                summary = "From Room",
                imageUrl = null,
                newsSite = "NASA",
                publishedAt = "2026-01-01",
                url = "https://example.com/7",
            ),
        )

        val article = repo.getCachedArticle(7)

        assertEquals("Cached", article?.title)
        assertEquals("From Room", article?.summary)
    }

    @Test
    fun `getCachedArticle returns null when absent`() = runTest {
        assertNull(repository().getCachedArticle(404))
    }
}
