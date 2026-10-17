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
 * Unit test del [ArticlesListViewModel] en JVM puro: repositorio y analytics se reemplazan con
 * MockK, el `viewModelScope` se controla con [MainDispatcherRule] y el Paging queda stubbeado con
 * `flowOf(PagingData.empty())`.
 *
 * **Dobles usados:**
 * - `mockk<ArticlesRepository>()` + `every { … } returns flowOf(PagingData.empty())` → STUB:
 *   respuesta enlatada; en varios tests ni se consume (el Flow `articles` es lazy y no se colecta).
 *   Nunca se verifica, así que nunca pasa a MOCK.
 * - `analytics = mockk<AnalyticsHelper>(relaxed = true)` → MOCK en `init logs screen view` y
 *   `sendAnalytics delegates to helper` (se verifica con `verify`); en el resto es STUB relajado
 *   que solo absorbe llamadas fire-and-forget (logScreenView del init) cuyas respuestas no importan.
 *
 * **Estilos (state/behavioral):** STATE para `searchQuery` y la exposición del Flow; BEHAVIORAL en
 * los tests que verifican el delegado de analytics.
 */
class ArticlesListViewModelTest {

    // Infraestructura de test (no doble): Main → TestDispatcher para el viewModelScope y el virtual
    // time de `runTest`.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // STATE — se assertea el valor inicial de `searchQuery`. El repo es STUB configurado de
    // antemano (aunque el Flow `articles` es lazy y acá no se colecta) y el analytics relajado solo
    // absorbe el logScreenView del init.
    @Test
    fun `initial states are correct`() = runTest {
        // DOBLE — STUB: respuesta enlatada; este test no la consume.
        val repository = mockk<ArticlesRepository>()
        // DOBLE — STUB relajado: absorbe el logScreenView del init; no se verifica en este test.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        assertEquals("", viewModel.searchQuery.value)
    }

    // BEHAVIORAL — verificación de interacción: el VM debe llamar a `logScreenView` al
    // construirse. El analytics relaxed actúa acá como MOCK (`verify`); el repo sigue de STUB.
    @Test
    fun `init logs screen view`() = runTest {
        // DOBLE — MOCK: recibe el logScreenView del init y se verifica con `verify`.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        // DOBLE — STUB: respuesta enlatada que este test no consume.
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        ArticlesListViewModel(repository, analytics)

        verify { analytics.logScreenView("ArticlesList") }
    }

    // STATE — assertea la exposición del Flow `articles` (no nulo) sin colectarlo; el STUB del
    // repo queda configurado pero sin consumir. No hay verificación de interacción.
    @Test
    fun `articles flow is lazy paging flow`() {
        // DOBLE — STUB: `flowOf(PagingData.empty())` queda enlatado; el Flow expuesto es lazy y no
        // se colecta en este test.
        val repository = mockk<ArticlesRepository>()
        // DOBLE — STUB relajado: absorbe el logScreenView del init sin asserciones.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        assertNotNull(viewModel.articles)
    }

    // BEHAVIORAL — el contrato es la delegación: `verify` sobre el MOCK de analytics comprueba que
    // se reenvió el evento con sus propiedades. El repo STUB no participa.
    @Test
    fun `sendAnalytics delegates to helper`() = runTest {
        // DOBLE — MOCK: se verifica la interacción con `verify`.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        // DOBLE — STUB: respuesta enlatada que este test no consume.
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        viewModel.sendAnalytics("test_event", mapOf("key" to "value"))

        verify { analytics.logEvent("test_event", mapOf("key" to "value")) }
    }

    // STATE — se llama al método y se assertea el valor resultante de `searchQuery`; verificación
    // de estado, sin interacciones.
    @Test
    fun `onSearchTextChange updates search query`() = runTest {
        // DOBLE — STUB: respuesta enlatada que este test no consume.
        val repository = mockk<ArticlesRepository>()
        // DOBLE — STUB relajado: absorbe el logScreenView del init sin asserciones.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        viewModel.onSearchTextChange("nasa")

        assertEquals("nasa", viewModel.searchQuery.value)
    }

    // STATE — tras `onSearchTextChange` + `clearSearch`, el estado observable vuelve a "". Mismo
    // estilo state que el resto de los tests de `searchQuery`.
    @Test
    fun `clearSearch resets search query`() = runTest {
        // DOBLE — STUB: respuesta enlatada que este test no consume.
        val repository = mockk<ArticlesRepository>()
        // DOBLE — STUB relajado: absorbe el logScreenView del init sin asserciones.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics)

        viewModel.onSearchTextChange("nasa")
        viewModel.clearSearch()

        assertEquals("", viewModel.searchQuery.value)
    }
}
