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

/**
 * Tests de [ArticlesListViewModel]: estado (`searchQuery`) como aserción primaria y verificación
 * de comportamiento para el delegado de analytics. El flow de paging (`articles`) no se colecta.
 *
 * Dobles usados:
 * - `mockk<ArticlesRepository>()` + `every { getArticlesPaged(any()) } returns flowOf(…)` → STUB
 *   (PagingData vacío enlatado; el flow frío no se colecta, así que además apenas satisface al
 *   constructor).
 * - `analytics = mockk<AnalyticsHelper>(relaxed = true)` → MOCK en los tests que verifican con
 *   `verify` (`init logs screen view`, `sendAnalytics delegates to helper`); STUB en el resto,
 *   donde responde defaults de `relaxed` sin verificación.
 *
 * Estilos (state/behavioral):
 * - STATE: `initial states are correct`, `articles flow is lazy paging flow` y los dos del search.
 * - BEHAVIORAL: `init logs screen view` y `sendAnalytics delegates to helper`.
 */
class ArticlesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // STATE — aserción del estado inicial (`searchQuery` vacío); el flow `articles` no se colecta.
    @Test
    fun `initial states are correct`() = runTest {
        // STUB — PagingData vacío enlatado; el flow frío `articles` no se colecta en estos tests,
        // así que el stub apenas satisface al constructor.
        val repository = mockk<ArticlesRepository>()
        // STUB — `relaxed` responde defaults: la VM llama a logScreenView en init, sin verificación.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        assertEquals("", viewModel.searchQuery.value)
    }

    // BEHAVIORAL — verifica la interacción del init: se registró el screen view "ArticlesList".
    @Test
    fun `init logs screen view`() = runTest {
        // MOCK — expectativa sobre interacciones: se verifica con `verify` en este test.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        ArticlesListViewModel(repository, analytics)

        verify { analytics.logScreenView("ArticlesList") }
    }

    // STATE — se assertea que el flow existe (no-null); no se colecta ni se avanza el debounce.
    @Test
    fun `articles flow is lazy paging flow`() {
        val repository = mockk<ArticlesRepository>()
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        assertNotNull(viewModel.articles)
    }

    // BEHAVIORAL — el delegado re-emite la llamada: analytics registra evento y propiedades
    // (el local `analytics` es un MOCK verificado con `verify`).
    @Test
    fun `sendAnalytics delegates to helper`() = runTest {
        // MOCK — expectativa sobre interacciones: se verifica con `verify` en este test.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        viewModel.sendAnalytics("test_event", mapOf("key" to "value"))

        verify { analytics.logEvent("test_event", mapOf("key" to "value")) }
    }

    // STATE — el StateFlow refleja el input: `onSearchTextChange("nasa")` ⇒ searchQuery "nasa".
    @Test
    fun `onSearchTextChange updates search query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        viewModel.onSearchTextChange("nasa")

        assertEquals("nasa", viewModel.searchQuery.value)
    }

    // STATE — transición del estado: "nasa" → clearSearch() ⇒ searchQuery vuelve a "".
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
