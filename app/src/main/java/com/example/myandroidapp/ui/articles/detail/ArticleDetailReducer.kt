package com.example.myandroidapp.ui.articles.detail

/**
 * Reducer **puro** del detalle: `(estado actual, resultado) -> estado nuevo`.
 *
 * Sin coroutines, sin dobles, sin efectos: se testea directamente (`ArticleDetailReducerTest`) y es
 * la única vía de mutación del estado de la pantalla.
 */
fun reduce(state: ArticleDetailUiState, result: ArticleDetailResult): ArticleDetailUiState =
    when (result) {
        ArticleDetailResult.Loading -> ArticleDetailUiState.Loading
        is ArticleDetailResult.Loaded -> ArticleDetailUiState.Content(result.article)
        is ArticleDetailResult.Failed -> ArticleDetailUiState.Error(result.message)
    }
