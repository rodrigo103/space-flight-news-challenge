package com.example.myandroidapp.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.RemoteMediator.MediatorResult.Success
import com.example.myandroidapp.data.mappers.toEntity
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.data.remote.configureApiClient
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.model.ArticleResponse
import com.example.myandroidapp.test.FakeArticleDao
import com.example.myandroidapp.test.RecordedRequests
import com.example.myandroidapp.test.testApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalPagingApi::class)
class ArticleRemoteMediatorTest {

    private val pageSize = 20

    private fun article(id: Int) = Article(
        id = id,
        title = "Article $id",
        summary = "Summary $id",
        url = "https://example.com/article$id",
        publishedAt = "2026-01-01T00:00:00Z",
    )

    private fun articlesJson(count: Int): String = Json.encodeToString(
        ArticleResponse(count = count, results = (1..count).map { article(it) }),
    )

    private fun failingApi(): ApiService {
        val engine = MockEngine { throw IOException("simulated network failure") }
        return ApiService(HttpClient(engine) { configureApiClient() })
    }

    private fun emptyState() = PagingState<Int, ArticleEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = pageSize),
        leadingPlaceholderCount = 0,
    )

    private fun stateWith(entities: List<ArticleEntity>) = PagingState<Int, ArticleEntity>(
        pages = listOf(PagingSource.LoadResult.Page(entities, prevKey = null, nextKey = null)),
        anchorPosition = null,
        config = PagingConfig(pageSize = pageSize),
        leadingPlaceholderCount = 0,
    )

    @Test
    fun `refresh on a full page inserts items and keeps pagination open`() = runTest {
        val recorded = RecordedRequests()
        val dao = FakeArticleDao()
        val api = testApiService(body = articlesJson(pageSize), recorded = recorded)

        val result = ArticleRemoteMediator(api, dao).load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertFalse((result as Success).endOfPaginationReached)
        assertEquals(pageSize, dao.inserted.size)
        assertEquals("Article 1", dao.inserted.first { it.id == 1 }.title)
        val url = recorded.urls.first()
        assertContains(url, "limit=$pageSize")
        assertContains(url, "offset=0")
    }

    @Test
    fun `refresh on a partial page marks end of pagination`() = runTest {
        val dao = FakeArticleDao()
        val api = testApiService(body = articlesJson(2))

        val result = ArticleRemoteMediator(api, dao).load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertTrue((result as Success).endOfPaginationReached)
        assertEquals(2, dao.inserted.size)
    }

    @Test
    fun `append uses the number of loaded items as offset`() = runTest {
        val recorded = RecordedRequests()
        val dao = FakeArticleDao()
        val api = testApiService(body = articlesJson(2), recorded = recorded)
        val loaded = (1..pageSize).map { article(it).toEntity() }

        val result = ArticleRemoteMediator(api, dao).load(LoadType.APPEND, stateWith(loaded))

        assertTrue(result is Success)
        assertContains(recorded.urls.first(), "offset=$pageSize")
    }

    @Test
    fun `append with an empty state falls back to offset zero`() = runTest {
        val recorded = RecordedRequests()
        val dao = FakeArticleDao()
        val api = testApiService(body = articlesJson(2), recorded = recorded)

        ArticleRemoteMediator(api, dao).load(LoadType.APPEND, emptyState())

        assertContains(recorded.urls.first(), "offset=0")
    }

    @Test
    fun `prepend succeeds without touching the network or the dao`() = runTest {
        val recorded = RecordedRequests()
        val dao = FakeArticleDao()
        val api = testApiService(recorded = recorded)

        val result = ArticleRemoteMediator(api, dao).load(LoadType.PREPEND, emptyState())

        assertTrue(result is Success)
        assertTrue((result as Success).endOfPaginationReached)
        assertEquals(0, recorded.urls.size)
        assertEquals(0, dao.inserted.size)
    }

    @Test
    fun `search query is sent as request parameter`() = runTest {
        val recorded = RecordedRequests()
        val api = testApiService(body = articlesJson(1), recorded = recorded)

        ArticleRemoteMediator(api, FakeArticleDao(), searchQuery = "mars")
            .load(LoadType.REFRESH, emptyState())

        assertContains(recorded.urls.first(), "search=mars")
    }

    @Test
    fun `http error returns MediatorResult Error`() = runTest {
        val api = testApiService(
            status = HttpStatusCode.InternalServerError,
            body = """{"error":"boom"}""",
        )

        val result = ArticleRemoteMediator(api, FakeArticleDao()).load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
    }

    @Test
    fun `network failure is swallowed as non-terminal success`() = runTest {
        val result = ArticleRemoteMediator(failingApi(), FakeArticleDao())
            .load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertFalse((result as Success).endOfPaginationReached)
    }
}
