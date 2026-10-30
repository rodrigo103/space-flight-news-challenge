package com.example.myandroidapp.ui.articles.detail

import com.example.myandroidapp.TestArticleData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests del **reducer puro** del detalle: `(estado, resultado) -> estado`.
 *
 * Sin coroutines, sin dobles, sin canales: cada transición de la pantalla queda documentada acá y
 * se testea como una función determinista.
 */
class ArticleDetailReducerTest {

    @Test
    fun `loading result resets content to loading`() {
        val next = reduce(
            state = ArticleDetailUiState.Content(TestArticleData.articleDetail),
            result = ArticleDetailResult.Loading,
        )

        assertEquals(ArticleDetailUiState.Loading, next)
    }

    @Test
    fun `loaded result builds content state`() {
        val next = reduce(
            state = ArticleDetailUiState.Loading,
            result = ArticleDetailResult.Loaded(TestArticleData.articleDetail),
        )

        assertEquals(ArticleDetailUiState.Content(TestArticleData.articleDetail), next)
    }

    @Test
    fun `failed result builds error state`() {
        val next = reduce(
            state = ArticleDetailUiState.Loading,
            result = ArticleDetailResult.Failed("boom"),
        )

        assertEquals(ArticleDetailUiState.Error("boom"), next)
    }

    @Test
    fun `loaded result replaces a previous error`() {
        val next = reduce(
            state = ArticleDetailUiState.Error("boom"),
            result = ArticleDetailResult.Loaded(TestArticleData.article1),
        )

        assertEquals(ArticleDetailUiState.Content(TestArticleData.article1), next)
    }
}
