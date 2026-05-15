package com.example.myandroidapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: Int,
    val title: String,
    val authors: List<Author> = emptyList(),
    val url: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("news_site") val newsSite: String? = null,
    val summary: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
)

@Serializable
data class Author(
    val name: String? = null,
    val socials: String? = null,
)

@Serializable
data class ArticleResponse(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<Article> = emptyList(),
)
