package com.example.myandroidapp.ui.articles.list

import androidx.paging.PagingData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.repository.ArticlesRepository
import com.example.myandroidapp.test.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class ArticlesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial states are correct`() = runTest {
        val repository = mockk<ArticlesRepository>()
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `init logs screen view`() = runTest {
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        ArticlesListViewModel(repository, analytics)

        verify { analytics.logScreenView("ArticlesList") }
    }

    @Test
    fun `articles flow is lazy paging flow`() {
        val repository = mockk<ArticlesRepository>()
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        assertNotNull(viewModel.articles)
    }

    @Test
    fun `sendAnalytics delegates to helper`() = runTest {
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        viewModel.sendAnalytics("test_event", mapOf("key" to "value"))

        verify { analytics.logEvent("test_event", mapOf("key" to "value")) }
    }

    @Test
    fun `onSearchTextChange updates search query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        viewModel.onSearchTextChange("nasa")

        assertEquals("nasa", viewModel.searchQuery.value)
    }

    @Test
    fun `clearSearch resets search query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        viewModel.onSearchTextChange("nasa")
        viewModel.clearSearch()

        assertEquals("", viewModel.searchQuery.value)
    }
}
