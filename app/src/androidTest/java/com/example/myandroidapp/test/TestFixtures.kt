package com.example.myandroidapp.test

import com.example.myandroidapp.data.Article

object TestFixtures {
    val article1 = Article(
        id = 1,
        title = "SpaceX Launches Starship",
        summary = "SpaceX successfully launched its Starship rocket from Boca Chica.",
        newsSite = "Space.com",
        publishedAt = "2026-05-15",
        url = "https://example.com/1",
    )

    val article2 = Article(
        id = 2,
        title = "NASA Mars Rover Update",
        summary = "Curiosity Rover discovers new evidence of ancient water on Mars.",
        newsSite = "NASA",
        publishedAt = "2026-05-14",
        url = "https://example.com/2",
    )

    val articleDetail = Article(
        id = 3,
        title = "Detailed Article Title",
        summary = "This is a comprehensive summary of the article with all details included.",
        newsSite = "ESA",
        publishedAt = "2026-05-13",
        url = "https://example.com/3",
        imageUrl = "https://example.com/img.jpg",
    )

    val articles = listOf(article1, article2)
}
