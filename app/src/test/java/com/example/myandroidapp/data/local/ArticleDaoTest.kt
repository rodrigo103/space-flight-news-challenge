package com.example.myandroidapp.data.local

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArticleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ArticleDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.articleDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: Int,
        title: String = "Title $id",
        summary: String = "Summary $id",
        publishedAt: String? = null,
    ) = ArticleEntity(
        id = id,
        title = title,
        summary = summary,
        imageUrl = null,
        newsSite = "NASA",
        publishedAt = publishedAt,
        url = "https://example.com/article$id",
    )

    private suspend fun loadAll(source: PagingSource<Int, ArticleEntity>): List<ArticleEntity> {
        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false),
        )
        return (result as PagingSource.LoadResult.Page).data
    }

    @Test
    fun `insertAll stores articles and getById returns them`() = runTest {
        dao.insertAll(listOf(entity(1), entity(2)))

        assertEquals("Title 1", dao.getById(1)?.title)
        assertEquals("Title 2", dao.getById(2)?.title)
    }

    @Test
    fun `getById returns null for unknown id`() = runTest {
        assertNull(dao.getById(99))
    }

    @Test
    fun `insert replaces existing article on conflict`() = runTest {
        dao.insert(entity(1, title = "Old"))
        dao.insertAll(listOf(entity(1, title = "New")))

        assertEquals("New", dao.getById(1)?.title)
    }

    @Test
    fun `clearAll removes every article`() = runTest {
        dao.insertAll(listOf(entity(1), entity(2)))

        dao.clearAll()

        assertEquals(emptyList<ArticleEntity>(), loadAll(dao.pagingSource()))
    }

    @Test
    fun `pagingSource orders by publishedAt descending`() = runTest {
        dao.insertAll(
            listOf(
                entity(1, publishedAt = "2026-01-01"),
                entity(2, publishedAt = "2026-01-03"),
                entity(3, publishedAt = "2026-01-02"),
            ),
        )

        assertEquals(listOf(2, 3, 1), loadAll(dao.pagingSource()).map { it.id })
    }

    @Test
    fun `searchPagingSource matches title or summary ignoring case`() = runTest {
        dao.insertAll(
            listOf(
                entity(1, title = "Mars mission", summary = "red planet", publishedAt = "2026-01-01"),
                entity(2, title = "Jupiter flyby", summary = "Mars orbiter", publishedAt = "2026-01-02"),
                entity(3, title = "Moon base", summary = "lunar habitat", publishedAt = "2026-01-03"),
            ),
        )

        assertEquals(listOf(2, 1), loadAll(dao.searchPagingSource("mars")).map { it.id })
        assertEquals(listOf(2, 1), loadAll(dao.searchPagingSource("MARS")).map { it.id })
    }

    @Test
    fun `searchPagingSource returns empty when nothing matches`() = runTest {
        dao.insertAll(listOf(entity(1, title = "Mars mission")))

        assertEquals(emptyList<ArticleEntity>(), loadAll(dao.searchPagingSource("venus")))
    }
}
