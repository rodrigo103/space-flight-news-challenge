package com.example.myandroidapp

import com.example.myandroidapp.domain.model.Article

/**
 * Fixtures (no dobles): instancias canónicas de [Article] reutilizadas por los tests unitarios que
 * necesitan un dominio válido sin construirlo a mano en cada caso.
 *
 * Son datos de entrada inertes: no devuelven respuestas ni registran llamadas, así que no
 * pertenecen a la taxonomía de test doubles.
 */
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
