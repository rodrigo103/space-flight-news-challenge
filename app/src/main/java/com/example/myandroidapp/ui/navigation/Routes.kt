package com.example.myandroidapp.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ArticlesRoute
@Serializable
data class DetailRoute(val articleId: Int)
