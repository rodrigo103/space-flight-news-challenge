package com.example.myandroidapp.ui.articles.detail

import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import com.example.myandroidapp.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Unit test del [ArticleDetailPaneViewModel] MVI en JVM puro.
 *
 * El artículo se pide con `onIntent(Load(id))` (no hay `SavedStateHandle` acá); el resto del flujo
 * es el mismo del detalle: Channel de intents → Channel de resultados → reducer puro → `state`.
 *
 * **Dobles usados:**
 * - `mockk<GetArticleUseCase>()` + `coEvery { … } returns` → STUB (respuestas enlatadas).
 * - `analytics = mockk<AnalyticsHelper>(relaxed = true)` → MOCK donde se verifica; STUB relajado
 *   en los tests de estado.
 *
 * **Estilos (state/behavioral):** STATE para las transiciones de `state`; BEHAVIORAL para las
 * verificaciones de analytics.
 */
class ArticleDetailPaneViewModelTest {

    // Infraestructura de test (no doble): Main → TestDispatcher.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // DOBLE — MOCK/STUB relajado según el test.
    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    private fun createViewModel(getArticle: GetArticleUseCase) =
        ArticleDetailPaneViewModel(getArticle = getArticle, analytics = analytics)

    private fun advance() = mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

    // STATE — estado inicial Loading, sin ningún intent emitido todavía.
    @Test
    fun `initial state is Loading`() {
        val viewModel = createViewModel(mockk(relaxed = true))

        assertEquals(ArticleDetailUiState.Loading, viewModel.state.value)
    }

    // STATE — Load exitoso: el reducer construye Content con el artículo del STUB.
    @Test
    fun `load success emits content state`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)
        val viewModel = createViewModel(getArticle)

        viewModel.onIntent(ArticleDetailPaneIntent.Load(articleId = 1))
        advance()

        assertEquals(
            ArticleDetailUiState.Content(TestArticleData.articleDetail),
            viewModel.state.value,
        )
    }

    // STATE — Load fallido: el reducer construye Error con el mensaje del STUB.
    @Test
    fun `load failure emits error state`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception("boom"))
        val viewModel = createViewModel(getArticle)

        viewModel.onIntent(ArticleDetailPaneIntent.Load(articleId = 1))
        advance()

        assertEquals(ArticleDetailUiState.Error("boom"), viewModel.state.value)
    }

    // STATE — fallback sin mensaje: el reducer produce "Unknown error".
    @Test
    fun `failure without message falls back to Unknown error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception())
        val viewModel = createViewModel(getArticle)

        viewModel.onIntent(ArticleDetailPaneIntent.Load(articleId = 1))
        advance()

        assertEquals(ArticleDetailUiState.Error("Unknown error"), viewModel.state.value)
    }

    // STATE — antes de avanzar el scheduler el intent no se procesó: sigue en Loading; después
    // pasa a Content. Verifica la transición completa.
    @Test
    fun `keeps Loading until the load resolves`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)
        val viewModel = createViewModel(getArticle)

        viewModel.onIntent(ArticleDetailPaneIntent.Load(articleId = 1))
        assertEquals(ArticleDetailUiState.Loading, viewModel.state.value)

        advance()
        assertEquals(
            ArticleDetailUiState.Content(TestArticleData.articleDetail),
            viewModel.state.value,
        )
    }

    // BEHAVIORAL — en éxito el pane registra screen view y evento (MOCK verificado).
    @Test
    fun `load success logs screen view and event`() = runTest {
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)
        val viewModel = ArticleDetailPaneViewModel(getArticle = getArticle, analytics = analytics)

        viewModel.onIntent(ArticleDetailPaneIntent.Load(articleId = 1))
        advance()

        verify { analytics.logScreenView("ArticleDetail_1") }
        verify { analytics.logEvent("article_loaded", mapOf("id" to "1")) }
    }
}
