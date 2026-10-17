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
 * Tests instrumentados de [ArticlesListScreen] con `createComposeRule`: se monta el composable
 * real con `ArticlesListAttributes`/`ArticlesListActions` fijos y se assertea el árbol de
 * semántica. Sin dobles: `PagingData` y fixtures son objetos reales.
 *
 * Dobles usados:
 * - Ninguno: composable real + `TestFixtures` (datos de prueba) + `emptyPagingData()` (fixture de
 *   PagingData real con load states) + `createComposeRule` (infraestructura de test).
 *
 * Estilos (state/behavioral):
 * - STATE: los tests de render (vacío, lista, sin resultados, news sites), leídos de la semántica.
 * - BEHAVIORAL: `searchBarUpdatesQuery`, `clearSearchButtonResetsQuery` y
 *   `clickingArticleCallsOnArticleClick` (interacciones que disparan callbacks registrados).
 */
class ArticlesListScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // STATE — estado vacío: con un PagingData sin items ya cargados se muestra "No articles available".
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

    // STATE — los títulos de los fixtures aparecen en la lista (árbol de semántica).
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

    // BEHAVIORAL — el input dispara onSearchTextChange: el `query` registrado refleja "mars".
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

    // BEHAVIORAL — el click en "Clear search" dispara onClearSearch: `query` vuelve a "".
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

    // BEHAVIORAL — el click en un título dispara onArticleClick con el id del artículo
    // (`clickedIds` registra y se assertea).
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

    // STATE — con searchQuery no vacío y sin items, el vacío que se muestra es el de búsqueda
    // ("No results found").
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

    // STATE — cada card muestra el news site del fixture ("Space.com", "NASA").
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
