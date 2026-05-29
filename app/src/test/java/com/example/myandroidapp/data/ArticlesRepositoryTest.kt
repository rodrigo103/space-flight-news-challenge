package com.example.myandroidapp.data

import com.example.myandroidapp.TestJson
import com.example.myandroidapp.data.local.ArticleDao
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.data.repository.DefaultArticlesRepository
import com.example.myandroidapp.rules.MockWebServerRule
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ArticlesRepositoryTest {

    @get:Rule
    val serverRule = MockWebServerRule()

    private fun createRepository(): DefaultArticlesRepository {
        val api = Retrofit.Builder()
            .baseUrl(serverRule.baseUrl())
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }
                    .asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(ApiService::class.java)
        val dao = mockk<ArticleDao>(relaxed = true)
        return DefaultArticlesRepository(api, dao)
    }

    @Test
    fun `getArticles on 200 returns success`() = runTest {
        serverRule.enqueueJson(200, TestJson.ARTICLE_RESPONSE)

        val result = createRepository().getArticles()

        assertTrue(result.isSuccess)
        val articles = result.getOrThrow()
        assertEquals(2, articles.size)
        assertEquals("Article 1", articles[0].title)
        assertEquals("Article 2", articles[1].title)
    }

    @Test
    fun `getArticles on 500 returns failure with HTTP code`() = runTest {
        serverRule.enqueueJson(500, """{"error":"Internal error"}""")

        val result = createRepository().getArticles()

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error is HttpException)
        assertEquals(500, (error as HttpException).code())
    }

    @Test
    fun `getArticles on 404 returns failure`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        val result = createRepository().getArticles()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun `getArticle on 200 returns success`() = runTest {
        serverRule.enqueueJson(200, TestJson.SINGLE_ARTICLE)

        val result = createRepository().getArticle(1)

        assertTrue(result.isSuccess)
        assertEquals("Article Detail", result.getOrThrow().title)
    }

    @Test
    fun `getArticle on 404 returns failure`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        val result = createRepository().getArticle(999)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun `getArticles on malformed JSON returns failure`() = runTest {
        serverRule.enqueueJson(200, """{broken json}""")

        val result = createRepository().getArticles()

        assertTrue(result.isFailure)
    }

    @Test
    fun `searchArticles on 200 returns filtered results`() = runTest {
        serverRule.enqueueJson(200, TestJson.SEARCH_RESPONSE)

        val result = createRepository().searchArticles("Article 1")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("Article 1", result.getOrThrow()[0].title)
    }

    @Test
    fun `getArticles with custom limit and offset`() = runTest {
        serverRule.enqueueJson(200, TestJson.ARTICLE_RESPONSE)

        createRepository().getArticles(limit = 10, offset = 5)

        val request = serverRule.server.takeRequest()
        assertEquals("10", request.requestUrl?.queryParameter("limit"))
        assertEquals("5", request.requestUrl?.queryParameter("offset"))
    }

    @Test
    fun `searchArticles on 500 returns failure`() = runTest {
        serverRule.enqueueJson(500, """{"error":"Server error"}""")

        val result = createRepository().searchArticles("test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun `searchArticles with empty results returns empty list`() = runTest {
        serverRule.enqueueJson(200, TestJson.EMPTY_RESPONSE)

        val result = createRepository().searchArticles("nonexistent")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().size)
    }
}
