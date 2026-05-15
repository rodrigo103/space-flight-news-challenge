package com.example.myandroidapp

import kotlinx.serialization.Serializable

@Serializable data object ArticlesRoute
@Serializable data class DetailRoute(val articleId: Int)
