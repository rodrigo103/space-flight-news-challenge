package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.test.TestArticleData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.test.MainDispatcherRule
import com.example.myandroidapp.ui.common.UiState
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests de [ArticleDetailViewModel]: el estado (`uiState`) es la aserción primaria; la
 * verificación de comportamiento se reserva para el evento de analytics (efecto one-shot).
 *
 * Dobles usados:
 * - `mockk<GetArticleUseCase>()` + `coEvery { … } returns ApiResult.…` → STUB (respuestas
 *   enlatadas); en `initial state is Loading` y en el test del SavedStateHandle queda DUMMY
 *   (se pasa pero nunca se usa: la carga no se avanza o el constructor falla antes).
 * - `analytics = mockk<AnalyticsHelper>(relaxed = true)` → MOCK en `logs event on success` (se
 *   verifica con `verify`); en el resto responde defaults de `relaxed` como STUB.
 * - `SavedStateHandle` → objeto real de infraestructura, no doble.
 * - `TestArticleData` → datos de prueba (fixture, no doble).
 *
 * Estilos (state/behavioral):
 * - STATE: `initial state is Loading`, `null error message…`, los 2 de loadArticle y
 *   `throws when articleId missing…`.
 * - BEHAVIORAL: `logs event on success` (solo la interacción con analytics).
 */
class ArticleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // MOCK — expectativas sobre interacciones: se verifica con `verify` en `logs event on success`
    // (ahí se usa un local que shadowea este campo). En el resto responde defaults de `relaxed`.
    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    // STATE — estado inicial (Loading) sin avanzar el scheduler: la carga queda encolada, así que
    // el use case DUMMY nunca se invoca.
    @Test
    fun `initial state is Loading`() {
        // DUMMY — se pasa al constructor y nunca se usa en este test.
        val getArticle = mockk<GetArticleUseCase>()

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )

        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    // STATE — fallback del mensaje: excepción sin message ⇒ "Unknown error" en el estado Error.
    @Test
    fun `null error message falls back to Unknown error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        // STUB — respuesta enlatada: `coEvery` fija el Result que devuelve el use case.
        coEvery { getArticle(1) } returns ApiResult.Failure(Exception())

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Unknown error", (state as UiState.Error).message)
    }

    // BEHAVIORAL — solo la interacción: analytics registró el evento `article_loaded` con el id
    // correcto (el local `analytics` es un MOCK verificado con `verify`). Sin asserts de estado.
    @Test
    fun `logs event on success`() = runTest {
        // MOCK — expectativa sobre interacciones: este test lo verifica con `verify`.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { analytics.logEvent("article_loaded", mapOf("id" to "1")) }
    }

    // STATE — Success con los datos: el article del STUB queda poblado en `ArticleDetailState`.
    @Test
    fun `loadArticle on success populates detail on success`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("Article Detail", data.article.title)
    }

    // STATE — Failure del STUB ⇒ estado Error con el mensaje de la excepción.
    @Test
    fun `loadArticle on failure sets error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns ApiResult.Failure(Exception("Not found"))

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Not found", (state as UiState.Error).message)
    }

    // STATE — contrato del constructor: sin `articleId` en el SavedStateHandle tira
    // IllegalStateException nombrando el dato faltante. El mock del use case es DUMMY (nunca se usa).
    @Test
    fun `throws when articleId missing from savedStateHandle`() {
        // DUMMY — el constructor falla antes de usarlo.
        val getArticle = mockk<GetArticleUseCase>()

        val exception = assertThrows(IllegalStateException::class.java) {
            ArticleDetailViewModel(
                getArticle = getArticle,
                analytics = analytics,
                savedStateHandle = SavedStateHandle(),
            )
        }
        assertTrue(exception.message?.contains("articleId") == true)
    }
}
