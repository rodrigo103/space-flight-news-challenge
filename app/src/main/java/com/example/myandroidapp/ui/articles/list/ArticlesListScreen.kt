package com.example.myandroidapp.ui.articles.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.example.myandroidapp.R
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.ui.components.ShimmerArticleCard
import com.example.myandroidapp.ui.components.ShimmerPage
import androidx.compose.material.icons.filled.Search as SearchIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesListScreen(
    state: ArticlesListState,
    onEvent: (ArticlesListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val articlesFlow = state.articles
    val snackbarHostState = remember { SnackbarHostState() }
    val unknownError = stringResource(R.string.unknown_error)

    if (articlesFlow == null) {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.space_flight_news),
                            modifier = Modifier.semantics { heading() },
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            GradientBackground(
                topColor = MaterialTheme.colorScheme.primaryContainer,
                bottomColor = MaterialTheme.colorScheme.inverseOnSurface,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(padding)) {
                    SearchField(
                        query = state.searchQuery,
                        onQueryChange = {
                            onEvent(ArticlesListEvent.SearchQueryChanged(it))
                        },
                        onClear = { onEvent(ArticlesListEvent.ClearSearch) },
                    )
                    ShimmerPage()
                }
            }
        }
        return
    }

    val articles = articlesFlow.collectAsLazyPagingItems()

    LaunchedEffect(articles.loadState) {
        val refreshError = articles.loadState.refresh as? LoadState.Error
        val appendError = articles.loadState.append as? LoadState.Error
        val error = refreshError ?: appendError
        val hasCachedItems = articles.itemCount > 0

        if (refreshError != null && !hasCachedItems) return@LaunchedEffect

        error?.let {
            snackbarHostState.showSnackbar(it.error.message ?: unknownError)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.space_flight_news),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        GradientBackground(
            topColor = MaterialTheme.colorScheme.primaryContainer,
            bottomColor = MaterialTheme.colorScheme.inverseOnSurface,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(padding)) {
                SearchField(
                    query = state.searchQuery,
                    onQueryChange = { onEvent(ArticlesListEvent.SearchQueryChanged(it)) },
                    onClear = { onEvent(ArticlesListEvent.ClearSearch) },
                )

                when (val refreshState = articles.loadState.refresh) {
                    is LoadState.Loading -> {
                        if (articles.itemCount == 0) {
                            ShimmerPage()
                        } else {
                            ArticleLazyList(articles, onEvent)
                        }
                    }

                    is LoadState.Error -> {
                        if (articles.itemCount == 0) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = refreshState.error.message
                                        ?: stringResource(R.string.unknown_error),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.Button(
                                    onClick = { articles.retry() },
                                ) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        } else {
                            ArticleLazyList(articles, onEvent)
                        }
                    }

                    else -> {
                        if (articles.itemCount == 0 &&
                            articles.loadState.refresh is LoadState.NotLoading
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (state.searchQuery.isNotEmpty()) {
                                        stringResource(R.string.no_results_found)
                                    } else {
                                        stringResource(R.string.no_articles_available)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            ArticleLazyList(articles, onEvent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val searchLabel = stringResource(R.string.search_articles)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { contentDescription = searchLabel },
        placeholder = { Text(searchLabel) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.SearchIcon,
                contentDescription = stringResource(R.string.search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
private fun ArticleLazyList(
    articles: androidx.paging.compose.LazyPagingItems<Article>,
    onEvent: (ArticlesListEvent) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = articles.itemCount,
            key = articles.itemKey { it.id },
            contentType = articles.itemContentType { "article" },
        ) { index ->
            val article = articles[index]
            if (article != null) {
                articleCardSettings(
                    article = article,
                    onClick = {
                        onEvent(ArticlesListEvent.ArticleClicked(article.id))
                    },
                )()
            }
        }
        if (articles.loadState.append is LoadState.Loading) {
            item {
                ShimmerArticleCard(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun GradientBackground(
    topColor: Color,
    bottomColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        color = containerColor,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val gradient = Brush.verticalGradient(
                        0.0f to topColor,
                        1.0f to bottomColor,
                    )
                    onDrawBehind { drawRect(gradient) }
                },
        ) {
            content()
        }
    }
}

@Composable
internal fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = accessibilityDescription(article)
                role = Role.Button
            }
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    article.newsSite?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    article.publishedAt?.let {
                        Text(
                            text = it.take(DATE_LENGTH),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private const val DATE_LENGTH = 10

private fun accessibilityDescription(article: Article): String = buildString {
    append(article.title)
    append(". ")
    append(article.summary)
    article.newsSite?.let {
        append(". Source: ")
        append(it)
    }
}
