package com.example.myandroidapp.data.mappers

import com.example.myandroidapp.data.local.ArticleEntity
import com.example.myandroidapp.domain.model.Article

fun ArticleEntity.toArticle() = Article(
    id = id,
    title = title,
    summary = summary,
    imageUrl = imageUrl,
    newsSite = newsSite,
    publishedAt = publishedAt,
    url = url,
)

fun Article.toEntity() = ArticleEntity(
    id = id,
    title = title,
    summary = summary,
    imageUrl = imageUrl,
    newsSite = newsSite,
    publishedAt = publishedAt,
    url = url,
)
