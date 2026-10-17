package com.example.myandroidapp.data.mappers

import com.example.myandroidapp.data.local.ArticleEntity
import com.example.myandroidapp.domain.model.Article
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests de los mappers [ArticleEntity] ↔ [Article]: transformaciones puras campo a campo, con
 * los datos de entrada construidos a mano en cada test.
 *
 * Dobles usados:
 * - Ninguno: el SUT son funciones puras (mappers) y los inputs son fixtures inline; no hay
 *   colaboradores que doblar.
 *
 * Estilos (state/behavioral):
 * - STATE: los 4 tests — tabla de casos entrada → salida, sin interacciones que verificar.
 */
class ArticleMappersTest {

    // STATE — tabla de casos: cada campo de la entity llega al article de dominio.
    @Test
    fun `toArticle maps all fields correctly`() {
        val entity = ArticleEntity(
            id = 1,
            title = "Test Title",
            summary = "Test Summary",
            imageUrl = "https://example.com/img.jpg",
            newsSite = "NASA",
            publishedAt = "2026-01-01T00:00:00Z",
            url = "https://example.com/article",
        )

        val article = entity.toArticle()

        assertEquals(1, article.id)
        assertEquals("Test Title", article.title)
        assertEquals("Test Summary", article.summary)
        assertEquals("https://example.com/img.jpg", article.imageUrl)
        assertEquals("NASA", article.newsSite)
        assertEquals("2026-01-01T00:00:00Z", article.publishedAt)
        assertEquals("https://example.com/article", article.url)
    }

    // STATE — nulls preservados: los campos opcionales nulos quedan nulos en el dominio.
    @Test
    fun `toArticle handles null fields`() {
        val entity = ArticleEntity(
            id = 2,
            title = "No extras",
            summary = "",
            imageUrl = null,
            newsSite = null,
            publishedAt = null,
            url = "",
        )

        val article = entity.toArticle()

        assertNull(article.imageUrl)
        assertNull(article.newsSite)
        assertNull(article.publishedAt)
    }

    // STATE — dirección inversa: cada campo del dominio llega a la entity.
    @Test
    fun `toEntity maps all fields correctly`() {
        val article = Article(
            id = 1,
            title = "Roundtrip",
            summary = "Roundtrip summary",
            imageUrl = "https://example.com/img.png",
            newsSite = "SpaceX",
            publishedAt = "2025-12-31",
            url = "https://example.com/rt",
        )

        val entity = article.toEntity()

        assertEquals(article.id, entity.id)
        assertEquals(article.title, entity.title)
        assertEquals(article.summary, entity.summary)
        assertEquals(article.imageUrl, entity.imageUrl)
        assertEquals(article.newsSite, entity.newsSite)
        assertEquals(article.publishedAt, entity.publishedAt)
        assertEquals(article.url, entity.url)
    }

    // STATE — roundtrip: entity → article → entity conserva el valor (la data class compara
    // por valor).
    @Test
    fun `roundtrip entity to article to entity preserves data`() {
        val entity = ArticleEntity(
            id = 42,
            title = "Roundtrip test",
            summary = "Summary",
            imageUrl = "https://img.example.com/1.jpg",
            newsSite = "Ars Technica",
            publishedAt = "2026-06-01",
            url = "https://example.com/rtt",
        )

        val article = entity.toArticle()
        val roundtripped = article.toEntity()

        assertEquals(entity, roundtripped)
    }
}
