package com.example.myandroidapp

import com.example.myandroidapp.domain.model.Article

object TestArticleData {
    val article1 = Article(
        id = 1, title = "Article 1", summary = "Summary 1",
        newsSite = "NASA", publishedAt = "2026-05-15",
    )
    val article2 = Article(
        id = 2, title = "Article 2", summary = "Summary 2",
        newsSite = "SpaceX", publishedAt = "2026-05-14",
    )
    val articleDetail = Article(
        id = 1, title = "Article Detail", summary = "Full summary",
        newsSite = "NASA", publishedAt = "2026-05-15",
    )
}
