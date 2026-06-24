package com.example.myandroidapp.ui.articles.list

import androidx.paging.PagingData
import com.example.myandroidapp.domain.model.Article
import kotlinx.coroutines.flow.Flow

data class ArticlesListAttributes(
    val searchQuery: String,
    val articles: Flow<PagingData<Article>>,
)

data class ArticlesListActions(
    val onSearchTextChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onArticleClick: (Int) -> Unit,
    val sendAnalytics: (String, Map<String, String>) -> Unit,
)

sealed interface ArticlesListSideEffect {
    data class NavigateToDetail(val articleId: Int) : ArticlesListSideEffect
}
