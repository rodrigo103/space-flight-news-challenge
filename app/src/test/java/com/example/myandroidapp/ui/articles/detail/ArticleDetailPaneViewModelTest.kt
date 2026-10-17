package com.example.myandroidapp.ui.articles.detail

import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.test.MainDispatcherRule
import com.example.myandroidapp.test.TestArticleData
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
 * Tests de [ArticleDetailPaneViewModel]: la aserción primaria es el estado (`uiState`); la
 * verificación de comportamiento (interacciones) se reserva para analytics, un efecto one-shot
 * que sí importa.
 *
 * Dobles usados:
 * - `mockk<GetArticleUseCase>()` con `coEvery { … } returns ApiResult.…` → STUB: respuestas
 *   enlatadas. Un mockk usado SOLO para devolver valores fijos es un stub; recién al verificarlo
 *   con `verify` se usa como MOCK.
 * - `analytics = mockk<AnalyticsHelper>(relaxed = true)` → MOCK: expectativas sobre interacciones
 *   (`verify { analytics.logScreenView(…) }`) — la herramienta del behavior testing.
 * - `mockk(relaxed = true)` inline en `initial state is Loading` → DUMMY: se pasa al constructor
 *   y nunca se usa.
 * - `TestArticleData` → datos de prueba (fixture compartida, no doble).
 *
 * Estilos (state/behavioral):
 * - STATE: `initial state is Loading`, `loadArticle failure without message…` y
 *   `loadArticle keeps Loading…`.
 * - STATE + BEHAVIORAL: `loadArticle success…` (estado + logScreenView/logEvent) y
 *   `loadArticle failure emits Error…` (estado + logError).
 */
class ArticleDetailPaneViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // MOCK — expectativas sobre interacciones: los tests verifican logScreenView/logEvent/logError
    // con `verify`. `relaxed = true` evita stubear los métodos que el SUT no llega a llamar.
    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    // STATE — aserción sobre el estado inicial (`Loading`), sin disparar ningún load.
    // El `mockk(relaxed = true)` inline es un DUMMY: se pasa al constructor y nunca se usa.
    @Test
    fun `initial state is Loading`() {
        val viewModel = ArticleDetailPaneViewModel(mockk(relaxed = true), analytics)

        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    // STATE + BEHAVIORAL — estado (Success con el artículo) + interacciones que importan:
    // analytics registró el screen view y el evento `article_loaded` con el id correcto.
    @Test
    fun `loadArticle success emits Success with the article`() = runTest {
        // STUB — respuestas enlatadas: `coEvery` fija qué devuelve el use case para el id 1.
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Success(TestArticleData.articleDetail)
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(TestArticleData.articleDetail, (state as UiState.Success).data.article)
        verify { analytics.logScreenView("ArticleDetail_1") }
        verify { analytics.logEvent("article_loaded", mapOf("id" to "1")) }
    }

    // STATE + BEHAVIORAL — estado (Error con el mensaje de la excepción) + el efecto one-shot:
    // analytics.logError con el contexto `loadArticle_1`.
    @Test
    fun `loadArticle failure emits Error with the message`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Failure(Exception("boom"))
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("boom", (state as UiState.Error).message)
        verify { analytics.logError(any(), "loadArticle_1") }
    }

    // STATE — solo el estado resultante: sin mensaje en la excepción, el fallback es "Unknown error".
    @Test
    fun `loadArticle failure without message falls back to Unknown error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Failure(Exception())
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown error", (viewModel.uiState.value as UiState.Error).message)
    }

    // STATE — verifica la transición completa: sigue en Loading antes de resolver y pasa a
    // Success tras `advanceUntilIdle()` (virtual time, sin sleeps reales).
    @Test
    fun `loadArticle keeps Loading until the request resolves`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Success(TestArticleData.articleDetail)
        val viewModel = ArticleDetailPaneViewModel(getArticle, analytics)

        viewModel.loadArticle(1)
        assertTrue(viewModel.uiState.value is UiState.Loading)

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)
    }
}
