package com.example.myandroidapp.ui.articles.list

import androidx.paging.PagingData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.data.preferences.AppPreferences
import com.example.myandroidapp.test.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ArticlesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = mockk<AnalyticsHelper>(relaxed = true)
    private val preferences = mockk<AppPreferences>(relaxed = true)

    @Test
    fun `initial states are correct`() = runTest {
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics, preferences)

        assertEquals("", viewModel.searchQuery.value)
        assertNull(viewModel.selectedArticleId.value)
    }

    @Test
    fun `init logs screen view`() = runTest {
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        ArticlesListViewModel(repository, analytics, preferences)

        verify { analytics.logScreenView("ArticlesList") }
    }

    @Test
    fun `articles flow is lazy paging flow`() {
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics, preferences)

        assertNotNull(viewModel.articles)
    }

    @Test
    fun `sendAnalytics delegates to helper`() = runTest {
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics, preferences)

        viewModel.sendAnalytics("test_event", mapOf("key" to "value"))

        verify { analytics.logEvent("test_event", mapOf("key" to "value")) }
    }

    @Test
    fun `onSearchTextChange updates search query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics, preferences)

        viewModel.onSearchTextChange("nasa")

        assertEquals("nasa", viewModel.searchQuery.value)
    }

    @Test
    fun `clearSearch resets search query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics, preferences)

        viewModel.onSearchTextChange("nasa")
        viewModel.clearSearch()

        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `onArticleSelected updates selectedArticleId`() = runTest {
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics, preferences)

        viewModel.onArticleSelected(42)

        assertEquals(42, viewModel.selectedArticleId.value)
    }
}
