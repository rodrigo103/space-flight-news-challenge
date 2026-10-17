package com.example.myandroidapp.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.RemoteMediator.MediatorResult.Success
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.myandroidapp.data.mappers.toEntity
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.model.ArticleResponse
import com.example.myandroidapp.rules.MockWebServerRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class ArticleRemoteMediatorTest {

    @get:Rule
    val serverRule = MockWebServerRule()

    private lateinit var db: AppDatabase
    private lateinit var dao: ArticleDao
    private lateinit var api: ApiService

    private val pageSize = 20

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.articleDao()
        api = Retrofit.Builder()
            .baseUrl(serverRule.baseUrl())
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }
                    .asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(ApiService::class.java)
    }

    @After
    fun tearDown() {
        db.close()
    }

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

    private fun mediator(search: String? = null) = ArticleRemoteMediator(api, dao, search)

    @Test
    fun `refresh on a full page inserts items and keeps pagination open`() = runTest {
        serverRule.enqueueJson(200, articlesJson(pageSize))

        val result = mediator().load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertFalse((result as Success).endOfPaginationReached)
        assertNotNull(dao.getById(1))
        assertNotNull(dao.getById(pageSize))
        val path = serverRule.server.takeRequest().path!!
        assertTrue(path.contains("limit=$pageSize"))
        assertTrue(path.contains("offset=0"))
    }

    @Test
    fun `refresh on a partial page marks end of pagination`() = runTest {
        serverRule.enqueueJson(200, articlesJson(2))

        val result = mediator().load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertTrue((result as Success).endOfPaginationReached)
        assertNotNull(dao.getById(2))
    }

    @Test
    fun `append uses the number of loaded items as offset`() = runTest {
        serverRule.enqueueJson(200, articlesJson(2))
        val loaded = (1..pageSize).map { article(it).toEntity() }

        val result = mediator().load(LoadType.APPEND, stateWith(loaded))

        assertTrue(result is Success)
        val path = serverRule.server.takeRequest().path!!
        assertTrue(path.contains("offset=$pageSize"))
    }

    @Test
    fun `append with an empty state falls back to offset zero`() = runTest {
        serverRule.enqueueJson(200, articlesJson(2))

        mediator().load(LoadType.APPEND, emptyState())

        val path = serverRule.server.takeRequest().path!!
        assertTrue(path.contains("offset=0"))
    }

    @Test
    fun `prepend succeeds without touching the network`() = runTest {
        val result = mediator().load(LoadType.PREPEND, emptyState())

        assertTrue(result is Success)
        assertTrue((result as Success).endOfPaginationReached)
        assertEquals(0, serverRule.server.requestCount)
    }

    @Test
    fun `search query is sent as request parameter`() = runTest {
        serverRule.enqueueJson(200, articlesJson(1))

        mediator(search = "mars").load(LoadType.REFRESH, emptyState())

        val path = serverRule.server.takeRequest().path!!
        assertTrue(path.contains("search=mars"))
    }

    @Test
    fun `http error returns MediatorResult Error`() = runTest {
        serverRule.enqueueJson(500, "{}")

        val result = mediator().load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
    }

    @Test
    fun `network failures are swallowed as non-terminal success`() = runTest {
        val exceptions = listOf(
            UnknownHostException("offline"),
            ConnectException("refused"),
            SocketTimeoutException("timeout"),
        )
        for (exception in exceptions) {
            val failingApi = mockk<ApiService>()
            coEvery { failingApi.getArticles(any(), any(), any()) } throws exception

            val result = ArticleRemoteMediator(failingApi, dao).load(LoadType.REFRESH, emptyState())

            assertTrue("Expected Success for $exception", result is Success)
            assertFalse((result as Success).endOfPaginationReached)
        }
    }
}
