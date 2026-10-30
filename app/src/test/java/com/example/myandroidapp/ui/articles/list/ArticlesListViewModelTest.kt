package com.example.myandroidapp.ui.articles.list

import app.cash.turbine.test
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
 * Unit test del [ArticlesListViewModel] MVI en JVM puro.
 *
 * Flujo bajo test: `onIntent()` → Channel de intents (serializado) → Channel de resultados →
 * reducer puro → `state`. Los efectos one-shot se consumen con Turbine.
 *
 * **Dobles usados:**
 * - `mockk<ArticlesRepository>()` + `every { … } returns flowOf(PagingData.empty())` → STUB: la
 *   respuesta del paging queda enlatada; en la mayoría de los tests ni se colecta.
 * - `analytics = mockk<AnalyticsHelper>(relaxed = true)` → MOCK donde se verifica con `verify`
 *   (`init logs screen view`, `send analytics delegates`); STUB relajado en el resto (absorbe los
 *   logScreenView/logEvent fire-and-forget).
 *
 * **Estilos (state/behavioral):** STATE para las transiciones de `state` vía intents; BEHAVIORAL
 * para el efecto one-shot del click y para las interacciones de analytics.
 */
class ArticlesListViewModelTest {

    // Infraestructura de test (no doble): Main → TestDispatcher; se comparte el scheduler con
    // `runTest` para poder drenar los Channels con `advanceUntilIdle()`.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // DOBLE — MOCK/STUB según el test: se verifica en los tests de analytics; en el resto solo
    // absorbe las llamadas del VM.
    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    // DOBLE — STUB: respuesta enlatada del paging; no se verifica.
    private val repository = mockk<ArticlesRepository>()

    private fun createViewModel(): ArticlesListViewModel {
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())
        return ArticlesListViewModel(repository, analytics)
    }

    // STATE — el estado inicial es un único objeto inmutable con la búsqueda vacía.
    @Test
    fun `initial state has empty search query`() = runTest {
        val viewModel = createViewModel()

        assertEquals(ArticlesListUiState(searchQuery = ""), viewModel.state.value)
    }

    // BEHAVIORAL — el VM registra el screen view al construirse (analytics es MOCK acá).
    @Test
    fun `init logs screen view`() = runTest {
        createViewModel()

        verify { analytics.logScreenView("ArticlesList") }
    }

    // STATE — el intent llega al Channel, el reducer lo aplica y el estado cambia: aserción de
    // estado final, sin verificar interacciones.
    @Test
    fun `search query intent updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(ArticlesListIntent.SearchQueryChanged("nasa"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("nasa", viewModel.state.value.searchQuery)
    }

    // STATE — ClearSearch vuelve la query a vacío; misma verificación de estado.
    @Test
    fun `clear search intent resets query`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(ArticlesListIntent.SearchQueryChanged("nasa"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(ArticlesListIntent.ClearSearch)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.state.value.searchQuery)
    }

    // BEHAVIORAL — el click NO muta el estado: emite un efecto one-shot que la View consume
    // (Turbine actúa como colector del canal de efectos).
    @Test
    fun `article click emits one-shot open effect without mutating state`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(ArticlesListIntent.ArticleClicked(articleId = 7))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(ArticlesListEffect.OpenArticle(articleId = 7), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(ArticlesListUiState(), viewModel.state.value)
    }

    // BEHAVIORAL — el intent de analytics delega en el helper (MOCK verificado con `verify`).
    @Test
    fun `send analytics delegates to helper`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(
            ArticlesListIntent.SendAnalytics("test_event", mapOf("key" to "value")),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { analytics.logEvent("test_event", mapOf("key" to "value")) }
    }

    // STATE — el Flow de paging se expone (lazy); no se colecta en este test.
    @Test
    fun `exposes lazy paging flow`() {
        val viewModel = createViewModel()

        assertNotNull(viewModel.articles)
    }
}
