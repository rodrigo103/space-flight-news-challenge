package com.example.myandroidapp.data.remote

import com.example.myandroidapp.test.RecordedRequests
import com.example.myandroidapp.test.TestJson
import com.example.myandroidapp.test.testApiService
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ApiServiceTest {

    @Test
    fun `getArticles returns parsed list on 200`() = runTest {
        val api = testApiService(body = TestJson.ARTICLE_RESPONSE)

        val response = api.getArticles()

        assertEquals(2, response.count)
        assertEquals(2, response.results.size)
        assertEquals("Article 1", response.results[0].title)
        assertEquals("NASA", response.results[0].newsSite)
        assertEquals("Article 2", response.results[1].title)
        assertEquals("SpaceX", response.results[1].newsSite)
    }

    @Test
    fun `getArticles maps 500 to ServerError`() = runTest {
        val api = testApiService(
            status = HttpStatusCode.InternalServerError,
            body = """{"error":"Internal error"}""",
        )

        val error = assertFailsWith<ApiException.ServerError> { api.getArticles() }

        assertEquals(500, error.code)
    }

    @Test
    fun `getArticles maps 404 to NotFound`() = runTest {
        val api = testApiService(status = HttpStatusCode.NotFound, body = """{"error":"Not found"}""")

        assertFailsWith<ApiException.NotFound> { api.getArticles() }
    }

    @Test
    fun `getArticles maps 400 to BadRequest`() = runTest {
        val api = testApiService(status = HttpStatusCode.BadRequest, body = """{"error":"Bad"}""")

        assertFailsWith<ApiException.BadRequest> { api.getArticles() }
    }

    @Test
    fun `getArticles maps 401 to Unauthorized`() = runTest {
        val api = testApiService(status = HttpStatusCode.Unauthorized, body = """{"error":"Nope"}""")

        assertFailsWith<ApiException.Unauthorized> { api.getArticles() }
    }

    @Test
    fun `error message carries status code and body`() = runTest {
        val api = testApiService(
            status = HttpStatusCode.NotFound,
            body = """{"error":"Not found"}""",
        )

        val error = assertFailsWith<ApiException.NotFound> { api.getArticles() }

        assertContains(error.message ?: "", "404")
        assertContains(error.message ?: "", "Not found")
    }

    @Test
    fun `getArticle returns parsed article on 200`() = runTest {
        val api = testApiService(body = TestJson.SINGLE_ARTICLE)

        val article = api.getArticle(1)

        assertEquals("Article Detail", article.title)
        assertEquals("Full summary", article.summary)
        assertEquals("NASA", article.newsSite)
    }

    @Test
    fun `getArticle maps 404 to NotFound`() = runTest {
        val api = testApiService(status = HttpStatusCode.NotFound, body = """{"error":"Not found"}""")

        assertFailsWith<ApiException.NotFound> { api.getArticle(999) }
    }

    @Test
    fun `getArticles handles null fields gracefully`() = runTest {
        val api = testApiService(
            body = """
            {
              "count": 1, "next": null, "previous": null,
              "results": [
                {
                  "id": 1, "title": "Article 1", "summary": "",
                  "image_url": null, "news_site": null, "published_at": null,
                  "url": "", "authors": []
                }
              ]
            }
            """.trimIndent(),
        )

        val article = api.getArticles().results[0]

        assertNull(article.imageUrl)
        assertNull(article.newsSite)
        assertNull(article.publishedAt)
    }

    @Test
    fun `getArticles sends limit offset and search as query params`() = runTest {
        val recorded = RecordedRequests()
        val api = testApiService(body = TestJson.ARTICLE_RESPONSE, recorded = recorded)

        api.getArticles(limit = 10, offset = 5, search = "mars")

        val url = recorded.urls.single()
        assertContains(url, "limit=10")
        assertContains(url, "offset=5")
        assertContains(url, "search=mars")
    }

    @Test
    fun `getArticles omits search param when null`() = runTest {
        val recorded = RecordedRequests()
        val api = testApiService(body = TestJson.ARTICLE_RESPONSE, recorded = recorded)

        api.getArticles(search = null)

        assertFalse(recorded.urls.single().contains("search="))
    }
}
