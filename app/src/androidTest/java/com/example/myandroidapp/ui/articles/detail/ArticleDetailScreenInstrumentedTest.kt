package com.example.myandroidapp.ui.articles.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.myandroidapp.test.TestFixtures
import com.example.myandroidapp.ui.common.UiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Test instrumentado de UI del [ArticleDetailScreen] con Compose: corre en device/emulador y
 * assertea sobre el árbol de semántica, alimentando la pantalla con estados reales de [UiState] y
 * los artículos de [TestFixtures].
 *
 * **Dobles usados:** ninguno — el composable es real, `createComposeRule()` es infraestructura y
 * los callbacks vacíos (`onBack = {}`) son no-ops, no dobles. [TestFixtures] son datos de prueba.
 *
 * **Estilos (state/behavioral):** STATE en los tests que assertean lo visible para cada estado
 * (Loading/Error/Success); BEHAVIORAL en `backButtonCallsOnBack`, que registra el callback y
 * verifica su invocación (el click en el árbol de semántica es el estímulo).
 */
class ArticleDetailScreenInstrumentedTest {

    // Infraestructura de test (no doble): regla de Compose que monta el contenido y sincroniza la
    // UI con el test.
    @get:Rule
    val composeTestRule = createComposeRule()

    // STATE — con `UiState.Loading` (estado real de entrada) se assertea la top bar visible en el
    // árbol de semántica. La Lottie de loading no se inspecciona; el assert es sobre lo mostrado.
    @Test
    fun showsLoadingState() {
        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Loading,
                ),
                actions = ArticleDetailActions(
                    onBack = {},
                ),
            )
        }

        // Loading state shows a Lottie animation; we verify the top bar is present
        composeTestRule.onNodeWithText("Article Details").assertIsDisplayed()
    }

    // STATE — el estado Error se refleja en la UI: el mensaje del `UiState` está visible en el
    // árbol de semántica. Sin dobles ni verificación de interacciones.
    @Test
    fun showsErrorState() {
        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Error("Failed to load article"),
                ),
                actions = ArticleDetailActions(
                    onBack = {},
                ),
            )
        }

        composeTestRule.onNodeWithText("Failed to load article").assertIsDisplayed()
    }

    // STATE — con Success y el fixture, se assertean los textos visibles (título, summary, sitio).
    // Aserciones sobre el árbol de semántica, no sobre composables internos.
    @Test
    fun showsArticleDetailOnSuccess() {
        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Success(
                        ArticleDetailState(article = TestFixtures.articleDetail)
                    ),
                ),
                actions = ArticleDetailActions(
                    onBack = {},
                ),
            )
        }

        composeTestRule.onNodeWithText("Detailed Article Title").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "This is a comprehensive summary of the article with all details included."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("ESA").assertIsDisplayed()
    }

    // BEHAVIORAL — el contrato es la interacción: el click en "Back" (content description) debe
    // invocar el callback registrado. La bandera `backCalled` verifica esa invocación; no hay
    // aserciones sobre la UI renderizada.
    @Test
    fun backButtonCallsOnBack() {
        var backCalled = false

        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Loading,
                ),
                actions = ArticleDetailActions(
                    onBack = { backCalled = true },
                ),
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backCalled)
    }

    // STATE — borde del render de Success: la fecha publicada sale formateada en pantalla
    // ("Published: ..."); aserción de estado visible, sin interacciones.
    @Test
    fun showsPublishedDateOnSuccess() {
        composeTestRule.setContent {
            ArticleDetailScreen(
                attributes = ArticleDetailAttributes(
                    state = UiState.Success(
                        ArticleDetailState(article = TestFixtures.articleDetail)
                    ),
                ),
                actions = ArticleDetailActions(
                    onBack = {},
                ),
            )
        }

        composeTestRule.onNodeWithText("Published: 2026-05-13").assertIsDisplayed()
    }
}
