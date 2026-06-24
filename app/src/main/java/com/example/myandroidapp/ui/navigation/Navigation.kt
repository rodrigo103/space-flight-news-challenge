package com.example.myandroidapp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myandroidapp.ui.articles.detail.ArticleDetailScreenRoute
import com.example.myandroidapp.ui.articles.list.ArticlesListScreenRoute

private const val NAV_ANIM_DURATION = 300

@Composable
fun MainNavigation(
    selectedArticleId: Int?,
    onArticleSelected: (Int) -> Unit,
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        selectedArticleId?.let { id ->
            navController.popBackStack(ArticlesRoute, inclusive = false)
            navController.navigate(DetailRoute(id))
        }
    }

    NavHost(navController = navController, startDestination = ArticlesRoute) {
        composable<ArticlesRoute>(
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(NAV_ANIM_DURATION)) { -it }
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(NAV_ANIM_DURATION)) { -it }
            }
        ) {
            ArticlesListScreenRoute(
                onNavigateToDetail = { articleId ->
                    onArticleSelected(articleId)
                    navController.navigate(DetailRoute(articleId))
                }
            )
        }
        composable<DetailRoute>(
            enterTransition = {
                slideInHorizontally(animationSpec = tween(NAV_ANIM_DURATION)) { it }
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(NAV_ANIM_DURATION)) { -it }
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(NAV_ANIM_DURATION)) { -it }
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(NAV_ANIM_DURATION)) { it }
            }
        ) {
            ArticleDetailScreenRoute(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
