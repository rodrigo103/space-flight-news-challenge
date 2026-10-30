package com.example.myandroidapp.ui.articles.list

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests del **reducer puro** de la lista: `(estado, resultado) -> estado`.
 *
 * Sin coroutines, sin dobles, sin canales: una función determinista. Este es el test más barato y
 * el que documenta la máquina de estados de la pantalla.
 */
class ArticlesListReducerTest {

    @Test
    fun `search query result updates the query`() {
        val next = reduce(
            state = ArticlesListUiState(searchQuery = ""),
            result = ArticlesListResult.SearchQueryUpdated("mars"),
        )

        assertEquals(ArticlesListUiState(searchQuery = "mars"), next)
    }

    @Test
    fun `search query result replaces a previous query`() {
        val next = reduce(
            state = ArticlesListUiState(searchQuery = "old"),
            result = ArticlesListResult.SearchQueryUpdated("new"),
        )

        assertEquals(ArticlesListUiState(searchQuery = "new"), next)
    }
}
