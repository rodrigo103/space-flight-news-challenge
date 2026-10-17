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
 * Tests instrumentados de [ArticleDetailScreen] con `createComposeRule`: se monta el composable
 * real con `ArticleDetailAttributes`/`ArticleDetailActions` fijos y se assertea el árbol de
 * semántica. Sin dobles: no hay colaboradores que doblar.
 *
 * Dobles usados:
 * - Ninguno: composable real + `TestFixtures` (datos de prueba, no dobles) + `createComposeRule`
 *   (infraestructura de test). Los lambdas vacíos de `actions` son placeholders, no dobles.
 *
 * Estilos (state/behavioral):
 * - STATE: los 4 tests de render (Loading/Error/Success/fecha), leídos del árbol de semántica.
 * - BEHAVIORAL: `backButtonCallsOnBack` (verifica el callback registrado en `actions`).
 */
class ArticleDetailScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // STATE — render del estado Loading: el top bar "Article Details" está en el árbol de semántica.
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

    // STATE — el mensaje del UiState.Error se renderiza en la pantalla.
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

    // STATE — Success (fixture real `TestFixtures.articleDetail`): título, summary y news site
    // aparecen en el árbol de semántica.
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

    // BEHAVIORAL — interacción: el click en "Back" dispara el callback registrado (`backCalled`).
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

    // STATE — Success: la fecha publicada formateada aparece en el árbol.
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
