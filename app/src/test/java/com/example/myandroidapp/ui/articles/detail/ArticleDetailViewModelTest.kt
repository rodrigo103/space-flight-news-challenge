package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit test del [ArticleDetailViewModel] en JVM puro: el UseCase se stubbea con MockK, el
 * `viewModelScope` se controla con [MainDispatcherRule] (virtual time) y el analytics se verifica
 * como mock en el test de logging.
 *
 * **Dobles usados:**
 * - `mockk<GetArticleUseCase>()` + `coEvery { … } returns …` → STUB: respuestas enlatadas, sin
 *   verificación de interacciones. Sin `coEvery` (tests que no lo ejercitan) funciona como DUMMY:
 *   se pasa al constructor y nunca se invoca.
 * - `analytics = mockk<AnalyticsHelper>(relaxed = true)` → MOCK en `logs event on success` (se
 *   verifica con `verify`; ahí el test usa una instancia local que shadowea la propiedad) y STUB
 *   relajado en el resto: absorbe llamadas fire-and-forget cuyas respuestas no importan.
 * - NO son dobles: `SavedStateHandle(...)` (objeto real de AndroidX) ni [MainDispatcherRule]
 *   (infraestructura de test).
 *
 * **Estilos (state/behavioral):** STATE para las transiciones de `uiState` (Loading →
 * Success/Error); BEHAVIORAL en `logs event on success` (verificación de interacción).
 */
class ArticleDetailViewModelTest {

    // Infraestructura de test (no doble): Main → TestDispatcher para controlar el virtual time del
    // viewModelScope con `advanceUntilIdle`.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // DOBLE — MOCK donde se verifica (`logs event on success`, con una instancia local) y STUB
    // relajado en los tests de estado: absorbe los logScreenView/logError del VM sin assertearlos.
    private val analytics = mockk<AnalyticsHelper>(relaxed = true)

    // STATE — solo importa el estado inicial del ViewModel. El UseCase es DUMMY: se pasa sin
    // `coEvery` y nunca se invoca (el `loadArticle()` del init queda encolado en el
    // StandardTestDispatcher de la rule y el test no avanza el scheduler).
    @Test
    fun `initial state is Loading`() {
        val getArticle = mockk<GetArticleUseCase>()

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )

        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    // STATE — el STUB devuelve un failure sin mensaje y se assertea el fallback del estado Error
    // ("Unknown error"). El analytics relajado solo absorbe el logError del camino de fallo.
    @Test
    fun `null error message falls back to Unknown error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception())

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

    // BEHAVIORAL — verificación de interacción: se usa un MOCK local (shadowea la propiedad) con
    // `verify` para assertear el evento de analytics. El STUB del UseCase solo aporta el éxito que
    // dispara el efecto.
    @Test
    fun `logs event on success`() = runTest {
        // DOBLE — MOCK: la instancia local que recibe el VM es la que se verifica con `verify`.
        val analytics = mockk<AnalyticsHelper>(relaxed = true)
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)

        val viewModel = ArticleDetailViewModel(
            getArticle = getArticle,
            analytics = analytics,
            savedStateHandle = SavedStateHandle(mapOf("articleId" to 1)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { analytics.logEvent("article_loaded", mapOf("id" to "1")) }
    }

    // STATE — con el STUB devolviendo éxito, se assertea la transición a Success y el contenido
    // del estado (título del artículo). Sin verificación de interacciones sobre el UseCase.
    @Test
    fun `loadArticle on success populates detail on success`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.success(TestArticleData.articleDetail)

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

    // STATE — el STUB enlata un fallo y se assertea el estado Error con su mensaje; el analytics
    // relajado absorbe el logError sin que el test lo verifique.
    @Test
    fun `loadArticle on failure sets error`() = runTest {
        val getArticle = mockk<GetArticleUseCase>()
        coEvery { getArticle(1) } returns Result.failure(Exception("Not found"))

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

    // STATE — contrato defensivo: sin `articleId` en el SavedStateHandle (objeto real), el
    // constructor lanza IllegalStateException. El UseCase es DUMMY: la construcción falla antes de
    // usarlo y el analytics tampoco llega a recibir el logScreenView del init.
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
        assertTrue(exception.message?.contains("articleId") == true)
    }
}
