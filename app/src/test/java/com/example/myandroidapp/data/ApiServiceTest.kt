package com.example.myandroidapp.data

import com.example.myandroidapp.TestJson
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.rules.MockWebServerRule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiServiceTest {

    @get:Rule
    val serverRule = MockWebServerRule()

    private val json = Json { ignoreUnknownKeys = true }

    private fun createApi(): ApiService {
        return Retrofit.Builder()
            .baseUrl(serverRule.baseUrl())
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }

    @Test
    fun `getArticles returns parsed list on 200`() = runTest {
        serverRule.enqueueJson(200, TestJson.ARTICLE_RESPONSE)

        val response = createApi().getArticles()

        assertEquals(2, response.count)
        assertEquals(2, response.results.size)
        assertEquals("Article 1", response.results[0].title)
        assertEquals("NASA", response.results[0].newsSite)
        assertEquals("Article 2", response.results[1].title)
        assertEquals("SpaceX", response.results[1].newsSite)
    }

    @Test
    fun `getArticles throws on 500`() = runTest {
        serverRule.enqueueJson(500, """{"error":"Internal error"}""")

        try {
            createApi().getArticles()
            throw AssertionError("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
    }

    @Test
    fun `getArticles throws on 404`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        try {
            createApi().getArticles()
            throw AssertionError("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    @Test
    fun `getArticle returns parsed article on 200`() = runTest {
        serverRule.enqueueJson(200, TestJson.SINGLE_ARTICLE)

        val article = createApi().getArticle(1)

        assertEquals("Article Detail", article.title)
        assertEquals("Full summary", article.summary)
        assertEquals("NASA", article.newsSite)
    }

    @Test
    fun `getArticle throws on 404`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        try {
            createApi().getArticle(999)
            throw AssertionError("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    @Test
    fun `getArticles handles null fields gracefully`() = runTest {
        serverRule.enqueueJson(
            200, """
            {
              "count": 1,
              "next": null,
              "previous": null,
              "results": [
                {
                  "id": 1,
                  "title": "Article 1",
                  "summary": "",
                  "image_url": null,
                  "news_site": null,
                  "published_at": null,
                  "url": "",
                  "authors": []
                }
              ]
            }
        """.trimIndent()
        )

        val article = createApi().getArticles().results[0]

        assertTrue(article.imageUrl == null)
        assertTrue(article.newsSite == null)
        assertTrue(article.publishedAt == null)
    }
}
