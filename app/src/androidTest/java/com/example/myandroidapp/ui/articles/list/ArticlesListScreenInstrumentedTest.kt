package com.example.myandroidapp.ui.articles.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.test.TestFixtures
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Test instrumentado de UI del [ArticlesListScreen] con Compose: corre en device/emulador y
 * assertea sobre el árbol de semántica. Las entradas son reales: `PagingData.from(...)` +
 * `flowOf(...)` (sin dobles) y [TestFixtures] como datos de prueba.
 *
 * **Dobles usados:** ninguno — composables, Flows y Paging reales; `createComposeRule()` es
 * infraestructura y el helper `emptyPagingData()` es un fixture de [PagingData] (no un doble).
 *
 * **Estilos (state/behavioral):** STATE en los tests que assertean lo visible para cada estado
 * (vacío, lista, sin resultados, sitio de la card); BEHAVIORAL en los tres que registran un
 * callback y verifican su invocación (input de búsqueda, limpiar búsqueda, click en artículo).
 */
class ArticlesListScreenInstrumentedTest {

    // Infraestructura de test (no doble): regla de Compose que monta el contenido y sincroniza la
    // UI con el test.
    @get:Rule
    val composeTestRule = createComposeRule()

    // STATE — con un PagingData real ya resuelto y vacío (fixture) se assertea el mensaje del
    // estado vacío en el árbol de semántica. Sin dobles ni verificación de interacciones.
    @Test
    fun showsEmptyStateWhenNoArticles() {
        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "",
                    articles = flowOf(emptyPagingData()),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No articles available").assertIsDisplayed()
    }

    // STATE — la lista paging real con los fixtures renderiza los títulos; aserción sobre nodos
    // visibles del árbol de semántica.
    @Test
    fun showsArticlesInList() {
        val pagingData = PagingData.from(TestFixtures.articles)

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "",
                    articles = flowOf(pagingData),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SpaceX Launches Starship").assertIsDisplayed()
        composeTestRule.onNodeWithText("NASA Mars Rover Update").assertIsDisplayed()
    }

    // BEHAVIORAL — verificación de interacción: el text input debe invocar `onSearchTextChange`
    // con el texto escrito. La lambda registrada muta el estado host y el assert comprueba esa
    // invocación (no hay aserción sobre la UI en sí).
    @Test
    fun searchBarUpdatesQuery() {
        var query by mutableStateOf("")

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = query,
                    articles = flowOf(PagingData.empty()),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = { query = it },
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.onNodeWithText("Search articles\u2026").performTextInput("mars")

        assertEquals("mars", query)
    }

    // BEHAVIORAL — el click en "Clear search" debe invocar `onClearSearch`, que acá se registra
    // reseteando el estado host; el assert sobre `query == ""` verifica esa interacción.
    @Test
    fun clearSearchButtonResetsQuery() {
        var query by mutableStateOf("nasa")

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = query,
                    articles = flowOf(PagingData.empty()),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = { query = it },
                    onClearSearch = { query = "" },
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.onNodeWithContentDescription("Clear search").performClick()

        assertEquals("", query)
    }

    // BEHAVIORAL — patrón `clickedIds.add(...)` + assert: se verifica que el click en la card
    // invoque el callback con el id correcto. Es verificación de interacción, no de UI renderizada.
    @Test
    fun clickingArticleCallsOnArticleClick() {
        val clickedIds = mutableListOf<Int>()
        val pagingData = PagingData.from(TestFixtures.articles)

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "",
                    articles = flowOf(pagingData),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = { clickedIds.add(it) },
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SpaceX Launches Starship").performClick()

        assertEquals(1, clickedIds.size)
        assertEquals(1, clickedIds.first())
    }

    // STATE — con `searchQuery` no vacío y paging vacío (fixture resuelto) se assertea el mensaje
    // "No results found": aserción de estado visible.
    @Test
    fun showsNoResultsWhenSearchHasNoMatches() {
        val pagingData = emptyPagingData()

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "nonexistent",
                    articles = flowOf(pagingData),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No results found").assertIsDisplayed()
    }

    // STATE — la card real con datos de fixture muestra los sitios esperados ("Space.com", "NASA");
    // aserción sobre el árbol de semántica.
    @Test
    fun showsCorrectNewsSiteOnArticleCard() {
        val pagingData = PagingData.from(TestFixtures.articles)

        composeTestRule.setContent {
            ArticlesListScreen(
                attributes = ArticlesListAttributes(
                    searchQuery = "",
                    articles = flowOf(pagingData),
                ),
                actions = ArticlesListActions(
                    onSearchTextChange = {},
                    onClearSearch = {},
                    onArticleClick = {},
                    sendAnalytics = { _, _ -> },
                ),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Space.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("NASA").assertIsDisplayed()
    }

    /**
     * A [PagingData] whose refresh load has already finished with no items, which is what a
     * search without matches produces in the app.
     *
     * `PagingData.empty()` on its own carries no load states, and a cold `flowOf(...)` does not
     * initialize the presenter the way the ViewModel's `cachedIn(...)` flow does, so
     * `LazyPagingItems` would stay on [LoadState.Loading] and the screen would render the loading
     * shimmer instead of the empty state.
     */
    private fun emptyPagingData(): PagingData<Article> = PagingData.empty(
        sourceLoadStates = LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = true),
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        ),
    )
}
