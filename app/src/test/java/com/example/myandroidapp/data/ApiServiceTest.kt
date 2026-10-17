package com.example.myandroidapp.data

import com.example.myandroidapp.TestJson
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.rules.MockWebServerRule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Test de contrato de red del [ApiService] en JVM: Retrofit + Kotlinx Serialization reales contra
 * un MockWebServer (servidor HTTP scriptable), sin device. Cubre el parseo de respuestas exitosas,
 * los errores HTTP (500/404) y los campos nulos del contrato.
 *
 * **Dobles usados:**
 * - [serverRule] → STUB a nivel HTTP: `enqueueJson` fija la respuesta enlatada que consume
 *   Retrofit; el archivo no llama a `takeRequest()`, así que no hay verificación de interacciones.
 * - NO son dobles: `createApi()` arma Retrofit/OkHttp/JSON reales apuntando al MockWebServer, y
 *   [TestJson] son datos de prueba.
 *
 * **Estilos (state/behavioral):** 100% STATE — se assertea el resultado observable (DTO parseado o
 * `HttpException` lanzada), nunca cómo lo obtuvo el SUT.
 */
class ApiServiceTest {

    // DOBLE — STUB (nivel HTTP): entrega respuestas enlatadas y fijas; cada test decide el
    // código/body con `enqueueJson` y el SUT (Retrofit) las consume. Acá no se inspeccionan las
    // requests, por lo que el server no cumple rol de spy.
    @get:Rule
    val serverRule = MockWebServerRule()

    private val json = Json { ignoreUnknownKeys = true }

    private fun createApi(): ApiService {
        return Retrofit.Builder()
            .baseUrl(serverRule.baseUrl())
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }

    // STATE — con un 200 enlatado por el STUB (TestJson) se assertea el DTO parseado campo a
    // campo: el resultado es el estado observable. No se verifica la request ni la interacción con
    // el stub.
    @Test
    fun `getArticles returns parsed list on 200`() = runTest {
        serverRule.enqueueJson(200, TestJson.ARTICLE_RESPONSE)

        val response = createApi().getArticles()

        assertEquals(2, response.count)
        assertEquals(2, response.results.size)
        assertEquals("Article 1", response.results[0].title)
        assertEquals("NASA", response.results[0].newsSite)
        assertEquals("Article 2", response.results[1].title)
        assertEquals("SpaceX", response.results[1].newsSite)
    }

    // STATE — el 500 se enlata con el stub y se assertea el outcome: la excepción resultante con
    // su código. Verificación de estado/error, no de llamadas.
    @Test
    fun `getArticles throws on 500`() = runTest {
        serverRule.enqueueJson(500, """{"error":"Internal error"}""")

        try {
            createApi().getArticles()
            throw AssertionError("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
    }

    // STATE — mismo contrato de error con 404: el resultado esperado es HttpException(404);
    // aserción sobre el outcome del SUT, sin verificar cómo se llamó al colaborador HTTP.
    @Test
    fun `getArticles throws on 404`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        try {
            createApi().getArticles()
            throw AssertionError("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    // STATE — caso single: mismo patrón que `getArticles`, el assert es sobre el artículo parseado
    // (título, summary y mapeo de `news_site`); el stub solo aporta el body.
    @Test
    fun `getArticle returns parsed article on 200`() = runTest {
        serverRule.enqueueJson(200, TestJson.SINGLE_ARTICLE)

        val article = createApi().getArticle(1)

        assertEquals("Article Detail", article.title)
        assertEquals("Full summary", article.summary)
        assertEquals("NASA", article.newsSite)
    }

    // STATE — borde del contrato de error para el endpoint single: 404 enlatado → HttpException
    // con código 404. Aserción de outcome, no de interacción.
    @Test
    fun `getArticle throws on 404`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        try {
            createApi().getArticle(999)
            throw AssertionError("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    // STATE — el stub devuelve un body con nulos explícitos y se assertea el parseo: los campos
    // opcionales quedan null en el DTO. Sigue siendo verificación de estado (el dato resultante).
    @Test
    fun `getArticles handles null fields gracefully`() = runTest {
        serverRule.enqueueJson(
            200, """
            {
              "count": 1,
              "next": null,
              "previous": null,
              "results": [
                {
                  "id": 1,
                  "title": "Article 1",
                  "summary": "",
                  "image_url": null,
                  "news_site": null,
                  "published_at": null,
                  "url": "",
                  "authors": []
                }
              ]
            }
        """.trimIndent()
        )

        val article = createApi().getArticles().results[0]

        assertTrue(article.imageUrl == null)
        assertTrue(article.newsSite == null)
        assertTrue(article.publishedAt == null)
    }
}
