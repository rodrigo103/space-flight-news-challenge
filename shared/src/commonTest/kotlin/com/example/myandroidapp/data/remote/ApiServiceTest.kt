package com.example.myandroidapp.data.remote

import com.example.myandroidapp.test.RecordedRequests
import com.example.myandroidapp.test.TestJson
import com.example.myandroidapp.test.testApiService
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Tests de [ApiService] contra el `MockEngine` que arma [testApiService]: parseo exitoso y mapeo
 * de status HTTP a [ApiException], sin backend real.
 *
 * Dobles usados:
 * - `testApiService(...)` → STUB (MockEngine de Ktor con respuestas HTTP enlatadas: status y body fijos).
 * - `RecordedRequests` → SPY (registra las URLs de las requests para assertear los query params).
 * - `TestJson.*` → datos de prueba (fixtures JSON, no dobles).
 *
 * Estilos (state/behavioral):
 * - STATE: el parseo y el mapeo de errores (asserts sobre la respuesta/excepción resultante).
 * - BEHAVIORAL: `getArticles sends limit offset and search as query params` y
 *   `getArticles omits search param when null` (verifican la URL con la que se llamó al engine).
 */
class ApiServiceTest {

    // STATE — solo el resultado del parseo: el STUB (200 + body enlatado) se deserializa y se
    // assertean campos del modelo.
    @Test
    fun `getArticles returns parsed list on 200`() = runTest {
        // STUB — MockEngine con respuesta HTTP enlatada: status y body fijos.
        val api = testApiService(body = TestJson.ARTICLE_RESPONSE)

        val response = api.getArticles()

        assertEquals(2, response.count)
        assertEquals(2, response.results.size)
        assertEquals("Article 1", response.results[0].title)
        assertEquals("NASA", response.results[0].newsSite)
        assertEquals("Article 2", response.results[1].title)
        assertEquals("SpaceX", response.results[1].newsSite)
    }

    // STATE — el mapeo de error resultante: un 500 enlatado (STUB) se convierte en
    // ApiException.ServerError con el código en `code`.
    @Test
    fun `getArticles maps 500 to ServerError`() = runTest {
        val api = testApiService(
            status = HttpStatusCode.InternalServerError,
            body = """{"error":"Internal error"}""",
        )

        val error = assertFailsWith<ApiException.ServerError> { api.getArticles() }

        assertEquals(500, error.code)
    }

    // STATE — mismo formato: 404 enlatado (STUB) ⇒ ApiException.NotFound.
    @Test
    fun `getArticles maps 404 to NotFound`() = runTest {
        val api = testApiService(status = HttpStatusCode.NotFound, body = """{"error":"Not found"}""")

        assertFailsWith<ApiException.NotFound> { api.getArticles() }
    }

    // STATE — mismo formato: 400 enlatado (STUB) ⇒ ApiException.BadRequest.
    @Test
    fun `getArticles maps 400 to BadRequest`() = runTest {
        val api = testApiService(status = HttpStatusCode.BadRequest, body = """{"error":"Bad"}""")

        assertFailsWith<ApiException.BadRequest> { api.getArticles() }
    }

    // STATE — mismo formato: 401 enlatado (STUB) ⇒ ApiException.Unauthorized.
    @Test
    fun `getArticles maps 401 to Unauthorized`() = runTest {
        val api = testApiService(status = HttpStatusCode.Unauthorized, body = """{"error":"Nope"}""")

        assertFailsWith<ApiException.Unauthorized> { api.getArticles() }
    }

    // STATE — contrato del mensaje de error: incluye el status code y el body del response
    // enlatado (STUB).
    @Test
    fun `error message carries status code and body`() = runTest {
        val api = testApiService(
            status = HttpStatusCode.NotFound,
            body = """{"error":"Not found"}""",
        )

        val error = assertFailsWith<ApiException.NotFound> { api.getArticles() }

        assertContains(error.message ?: "", "404")
        assertContains(error.message ?: "", "Not found")
    }

    // STATE — parseo del endpoint de detalle: asserts sobre el article deserializado del STUB.
    @Test
    fun `getArticle returns parsed article on 200`() = runTest {
        val api = testApiService(body = TestJson.SINGLE_ARTICLE)

        val article = api.getArticle(1)

        assertEquals("Article Detail", article.title)
        assertEquals("Full summary", article.summary)
        assertEquals("NASA", article.newsSite)
    }

    // STATE — el 404 también se mapea en getArticle: NotFound para un id inexistente.
    @Test
    fun `getArticle maps 404 to NotFound`() = runTest {
        val api = testApiService(status = HttpStatusCode.NotFound, body = """{"error":"Not found"}""")

        assertFailsWith<ApiException.NotFound> { api.getArticle(999) }
    }

    // STATE — deserialización defensiva: con campos opcionales null en el JSON del STUB, el
    // modelo llega con imageUrl/newsSite/publishedAt en null (sin explotar).
    @Test
    fun `getArticles handles null fields gracefully`() = runTest {
        val api = testApiService(
            body = """
            {
              "count": 1, "next": null, "previous": null,
              "results": [
                {
                  "id": 1, "title": "Article 1", "summary": "",
                  "image_url": null, "news_site": null, "published_at": null,
                  "url": "", "authors": []
                }
              ]
            }
            """.trimIndent(),
        )

        val article = api.getArticles().results[0]

        assertNull(article.imageUrl)
        assertNull(article.newsSite)
        assertNull(article.publishedAt)
    }

    // BEHAVIORAL — verifica la interacción: limit, offset y search viajan como query params en
    // la URL de la request (leída del SPY). Sin asserts de estado.
    @Test
    fun `getArticles sends limit offset and search as query params`() = runTest {
        // SPY — registra las URLs de las requests para assertear los query params.
        val recorded = RecordedRequests()
        val api = testApiService(body = TestJson.ARTICLE_RESPONSE, recorded = recorded)

        api.getArticles(limit = 10, offset = 5, search = "mars")

        val url = recorded.urls.single()
        assertContains(url, "limit=10")
        assertContains(url, "offset=5")
        assertContains(url, "search=mars")
    }

    // BEHAVIORAL — interacción negativa: con `search = null` la URL no lleva el param `search=`
    // (leído del SPY).
    @Test
    fun `getArticles omits search param when null`() = runTest {
        val recorded = RecordedRequests()
        val api = testApiService(body = TestJson.ARTICLE_RESPONSE, recorded = recorded)

        api.getArticles(search = null)

        assertFalse(recorded.urls.single().contains("search="))
    }
}
