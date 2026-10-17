package com.example.myandroidapp.ui.articles.detail

import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.test.MainDispatcherRule
import com.example.myandroidapp.ui.common.UiState
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit test del [ArticleDetailPaneViewModel] en JVM puro: el UseCase se stubbea con MockK, el
 * `viewModelScope` se controla con [MainDispatcherRule] (Main → TestDispatcher, virtual time) y
 * el analytics relajado se verifica como mock.
 *
 * **Dobles usados:**
 * - `mockk<GetArticleUseCase>()` + `coEvery { … } returns …` → STUB: respuestas enlatadas.
 *   Un mockk usado SOLO para devolver valores es un stub; se vuelve MOCK cuando se verifican
 *   sus interacciones.
 * - `analytics = mockk<AnalyticsHelper>(relaxed = true)` + `verify { … }` → MOCK: expectativas
 *   sobre interacciones; es la herramienta del behavior testing.
 * - `mockk(relaxed = true)` pasado al constructor en `initial state is Loading` → DUMMY: cubre
 *   el parámetro y el test nunca lo usa.
 *
 * **Estilos (state/behavioral):** las transiciones de `uiState` (Loading → Success/Error) se
 * assertean como STATE; los tests de analytics agregan BEHAVIORAL (efectos one-shot).
 */
class ArticleDetailPaneViewModelTest {

    // Infraestructura de test (no doble): reemplaza Dispatchers.Main por un TestDispatcher para
    // controlar el virtual time del viewModelScope con `advanceUntilIdle`.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // DOBLE — MOCK: `relaxed` evita fallar ante llamadas no configuradas y los tests lo usan con
    // `verify { … }` para comprobar expectativas sobre las interacciones (behavior testing).
    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    // STATE — solo importa el estado inicial del ViewModel. El use case se pasa como DUMMY
    // (`mockk(relaxed = true)`): este test nunca lo usa, está ahí para llenar el parámetro.
    @Test
    fun `initial state is Loading`() {
        val viewModel = ArticleDetailPaneViewModel(mockk(relaxed = true), analytics)

        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    // STATE + BEHAVIORAL (analytics) — el STUB devuelve el artículo y se assertea la transición
    // a Success; además se verifican efectos one-shot (logScreenView + logEvent), donde el mock
    // relajado sí actúa como MOCK.
    @Test
    fun `loadArticle success emits Success with the article`() = runTest {
        // STUB — mockk configurado con `coEvery { … } returns …`: respuesta enlatada, sin
        // verificación de interacciones sobre el UseCase.
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(TestArticleData.articleDetail, (state as UiState.Success).data.article)
        verify { analytics.logScreenView("ArticleDetail_1") }
        verify { analytics.logEvent("article_loaded", mapOf("id" to "1")) }
    }

    // STATE + BEHAVIORAL (analytics) — el STUB devuelve Result.failure y se assertea el estado
    // Error con el mensaje; se suma la verificación de `logError` como efecto colateral.
    @Test
    fun `loadArticle failure emits Error with the message`() = runTest {
        // STUB — fija el resultado de fallo; el test no verifica interacciones del UseCase.
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception("boom"))
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("boom", (state as UiState.Error).message)
        verify { analytics.logError(any(), "loadArticle_1") }
    }

    // STATE — caso borde del mapeo de error: excepción sin mensaje → "Unknown error". Sin
    // aserciones de interacción (el analytics relajado solo absorbe lo que la VM dispare).
    @Test
    fun `loadArticle failure without message falls back to Unknown error`() = runTest {
        // STUB — mismo patrón: respuesta enlatada, acá un failure sin mensaje.
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception())
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown error", (viewModel.uiState.value as UiState.Error).message)
    }

    // STATE — assertea la transición temporal Loading → Success controlando el virtual time del
    // TestDispatcher: antes de `advanceUntilIdle` sigue Loading, después Success.
    @Test
    fun `loadArticle keeps Loading until the request resolves`() = runTest {
        // STUB — respuesta enlatada que se resolverá recién al avanzar el scheduler.
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        assertTrue(viewModel.uiState.value is UiState.Loading)

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)
    }
}
