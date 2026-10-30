package com.example.myandroidapp.ui.articles.detail

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

/**
 * Unit test del [ArticleDetailViewModel] MVI en JVM puro.
 *
 * El `Retry` que dispara el `init` viaja por el Channel de intents y se procesa al avanzar el
 * virtual time ([MainDispatcherRule]). Los efectos one-shot se consumen con Turbine.
 *
 * **Dobles usados:**
 * - `mockk<GetArticleUseCase>()` + `coEvery { … } returns …` → STUB: respuestas enlatadas. Sin
 *   `coEvery` y sin avanzar el scheduler (test de estado inicial) actúa como DUMMY: nunca se
 *   invoca.
 * - `analytics = mockk<AnalyticsHelper>(relaxed = true)` → MOCK donde se verifica (`logs event on
 *   success`, `delete …`); STUB relajado en el resto (absorbe logScreenView/logError).
 * - NO son dobles: `SavedStateHandle(...)` ni [MainDispatcherRule].
 *
 * **Estilos (state/behavioral):** STATE para las transiciones de `state`; BEHAVIORAL para el
 * efecto de navegación del delete y para las interacciones de analytics.
 */
class ArticleDetailViewModelTest {

    // Infraestructura de test (no doble): Main → TestDispatcher para el viewModelScope.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // DOBLE — MOCK donde se verifica; STUB relajado en los tests de estado.
    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    private fun createViewModel(getArticle: GetArticleUseCase) = ArticleDetailViewModel(
        getArticle = getArticle,
        analytics = analytics,
        savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
    )

    private fun advance() = mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

    // STATE — el estado inicial es Loading; el `Retry` del init todavía no se procesó porque no se
    // avanzó el scheduler. El UseCase es DUMMY: se pasa y nunca se invoca en este test.
    @Test
    fun `initial state is Loading`() {
        val viewModel = createViewModel(mockk(relaxed = true))

        assertEquals(ArticleDetailUiState.Loading, viewModel.state.value)
    }

    // STATE — el STUB enlata un failure sin mensaje y el reducer produce el fallback "Unknown
    // error".
    @Test
    fun `null error message falls back to Unknown error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception())

        val viewModel = createViewModel(getArticle)
        advance()

        assertEquals(
            ArticleDetailUiState.Error("Unknown error"),
            viewModel.state.value,
        )
    }

    // BEHAVIORAL — el efecto one-shot de analytics se verifica con un MOCK local (`verify`).
    @Test
    fun `logs event on success`() = runTest {
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)
        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        advance()

        verify { analytics.logEvent("article_loaded", mapOf("id" to "1")) }
    }

    // STATE — éxito: el reducer construye Content con el artículo del STUB.
    @Test
    fun `successful load emits content state`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = createViewModel(getArticle)
        advance()

        assertEquals(
            ArticleDetailUiState.Content(TestArticleData.articleDetail),
            viewModel.state.value,
        )
    }

    // STATE — fallo: el reducer construye Error con el mensaje del STUB.
    @Test
    fun `failed load emits error state`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception("Not found"))

        val viewModel = createViewModel(getArticle)
        advance()

        assertEquals(ArticleDetailUiState.Error("Not found"), viewModel.state.value)
    }

    // BEHAVIORAL — Delete sobre contenido: NO muta el estado, emite NavigateBack (efecto one-shot)
    // y registra el evento de analytics (MOCK verificado).
    @Test
    fun `delete on content emits navigate back effect and logs event`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)
        val viewModel = createViewModel(getArticle)
        advance()

        viewModel.effects.test {
            viewModel.onIntent(ArticleDetailIntent.Delete)
            advance()

            assertEquals(ArticleDetailEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        verify { analytics.logEvent("article_deleted", mapOf("id" to "1")) }
    }

    // STATE + BEHAVIORAL (negativo) — Delete fuera de Content: no navega ni loguea. Se assertea
    // que NO hay eventos (estado) y que no hubo interacción (behavioral).
    @Test
    fun `delete outside content does nothing`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception("Not found"))
        val viewModel = createViewModel(getArticle)
        advance()

        viewModel.effects.test {
            viewModel.onIntent(ArticleDetailIntent.Delete)
            advance()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 0) { analytics.logEvent("article_deleted", any()) }
    }

    // STATE — contrato defensivo del constructor (SavedStateHandle real): sin `articleId` lanza
    // IllegalStateException. El UseCase es DUMMY (la construcción falla antes de usarlo).
    @Test
    fun `throws when articleId missing from savedStateHandle`() {
        val getArticle = mockk<GetArticleUseCase>()

        val exception = assertThrows(IllegalStateException::class.java) {
            ArticleDetailViewModel(
                getArticle = getArticle,
                analytics = analytics,
                savedStateHandle = SavedStateHandle(),
            )
        }
        org.junit.Assert.assertTrue(exception.message?.contains("articleId") == true)
    }
}
