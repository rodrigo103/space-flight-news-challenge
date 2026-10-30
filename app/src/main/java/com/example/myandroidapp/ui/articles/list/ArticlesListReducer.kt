package com.example.myandroidapp.ui.articles.list

/**
 * Reducer **puro** de la lista: `(estado actual, resultado) -> estado nuevo`.
 *
 * Sin coroutines, sin dobles, sin efectos: es una función determinista que se testea directamente
 * (`ArticlesListReducerTest`). Toda transición de estado de la pantalla pasa por acá.
 */
fun reduce(state: ArticlesListUiState, result: ArticlesListResult): ArticlesListUiState =
    when (result) {
        is ArticlesListResult.SearchQueryUpdated -> state.copy(searchQuery = result.query)
    }
