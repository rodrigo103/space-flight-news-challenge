package com.example.myandroidapp.test

object TestJson {
    val ARTICLE_RESPONSE = """
    {
      "count": 2,
      "next": null,
      "previous": null,
      "results": [
        {
          "id": 1,
          "title": "Article 1",
          "summary": "Summary 1",
          "image_url": "https://example.com/img1.jpg",
          "news_site": "NASA",
          "published_at": "2026-01-01T00:00:00Z",
          "url": "https://example.com/article1",
          "authors": []
        },
        {
          "id": 2,
          "title": "Article 2",
          "summary": "Summary 2",
          "image_url": null,
          "news_site": "SpaceX",
          "published_at": null,
          "url": "https://example.com/article2",
          "authors": []
        }
      ]
    }
    """.trimIndent()

    val SINGLE_ARTICLE = """
    {
      "id": 1,
      "title": "Article Detail",
      "summary": "Full summary",
      "image_url": "https://example.com/img.jpg",
      "news_site": "NASA",
      "published_at": "2026-01-01T00:00:00Z",
      "url": "https://example.com/detail",
      "authors": [{"name": "John Doe", "socials": null}]
    }
    """.trimIndent()

    val EMPTY_RESPONSE = """
    {"count": 0, "next": null, "previous": null, "results": []}
    """.trimIndent()

    val SEARCH_RESPONSE = """
    {
      "count": 1,
      "next": null,
      "previous": null,
      "results": [
        {
          "id": 1,
          "title": "Article 1",
          "summary": "Summary 1",
          "image_url": null,
          "news_site": "NASA",
          "published_at": "2026-01-01T00:00:00Z",
          "url": "https://example.com/article1",
          "authors": []
        }
      ]
    }
    """.trimIndent()
}
