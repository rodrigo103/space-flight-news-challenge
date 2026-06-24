package com.example.myandroidapp.ui.articles.list

import androidx.paging.PagingData
import com.example.myandroidapp.domain.model.Article
import kotlinx.coroutines.flow.Flow

data class ArticlesListState(
    val searchQuery: String = "",
    val articles: Flow<PagingData<Article>>? = null,
)

sealed interface ArticlesListEvent {
    data class SearchQueryChanged(val query: String) : ArticlesListEvent
    data object ClearSearch : ArticlesListEvent
    data class ArticleClicked(val articleId: Int) : ArticlesListEvent
}

sealed interface ArticlesListSideEffect {
    data class NavigateToDetail(val articleId: Int) : ArticlesListSideEffect
}
