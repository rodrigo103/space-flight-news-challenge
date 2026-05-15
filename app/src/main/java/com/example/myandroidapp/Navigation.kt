package com.example.myandroidapp

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myandroidapp.ui.articles.detail.ArticleDetailScreen
import com.example.myandroidapp.ui.articles.list.ArticlesListScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ArticlesRoute) {
        composable<ArticlesRoute> {
            ArticlesListScreen(onArticleClick = { articleId ->
                navController.navigate(DetailRoute(articleId))
            })
        }
        composable<DetailRoute> {
            ArticleDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
