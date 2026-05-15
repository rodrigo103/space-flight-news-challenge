package com.example.myandroidapp

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.myandroidapp.ui.articles.detail.ArticleDetailScreen
import com.example.myandroidapp.ui.articles.list.ArticlesListScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(ArticlesList)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<ArticlesList> {
                ArticlesListScreen(onArticleClick = { articleId ->
                    backStack.add(ArticleDetail(articleId))
                })
            }
            entry<ArticleDetail> { detail ->
                ArticleDetailScreen(articleId = detail.articleId, onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
