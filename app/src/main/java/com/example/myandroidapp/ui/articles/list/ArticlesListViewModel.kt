package com.example.myandroidapp.ui.articles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        analytics.logScreenView("ArticlesList")
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val articles: Flow<PagingData<Article>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            repository.getArticlesPaged(searchQuery = query.ifBlank { null })
        }
        .cachedIn(viewModelScope)

    fun onSearchTextChange(text: String) {
        _searchQuery.value = text
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun sendAnalytics(event: String, properties: Map<String, String>) {
        analytics.logEvent(event, properties)
    }
}
