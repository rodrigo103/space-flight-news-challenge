package com.example.myandroidapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object ArticlesList : NavKey

@Serializable data class ArticleDetail(val articleId: Int) : NavKey
