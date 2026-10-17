package com.example.myandroidapp.data.mappers

import com.example.myandroidapp.data.local.ArticleEntity
import com.example.myandroidapp.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Test puro de los mappers entity ↔ domain ([toArticle]/[toEntity]): sin dobles, sin Android,
 * corre en JVM. Es el patrón de tabla de casos de la capa de mappers (nota 28, §1.4).
 *
 * **Dobles usados:** ninguno — entradas y salidas son instancias reales de [ArticleEntity] y
 * [Article] construidas como datos de prueba. No hay colaboradores que stubear ni interacciones
 * que verificar.
 *
 * **Estilos (state/behavioral):** 100% STATE — se assertea el objeto resultante campo a campo (o
 * por igualdad de data classes en el roundtrip).
 */
class ArticleMappersTest {

    // STATE — aserción campo a campo del objeto mapeado; la entrada es un dato de prueba real.
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

    // STATE — borde del mapper: los campos opcionales nulos se preservan en el dominio; aserción
    // sobre el estado del objeto resultante.
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

    // STATE — dirección inversa (domain → entity): se assertea cada campo del resultado contra el
    // objeto de entrada. Mismo estilo state que `toArticle`.
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

    // STATE — roundtrip entity → article → entity: la aserción es la igualdad estructural del
    // data class final con el original. Cubre ambos mappers juntos sin interacciones.
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
