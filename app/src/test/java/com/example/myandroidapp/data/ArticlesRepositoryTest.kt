package com.example.myandroidapp.data

import com.example.myandroidapp.TestJson
import com.example.myandroidapp.data.local.ArticleDao
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.data.repository.DefaultArticlesRepository
import com.example.myandroidapp.rules.MockWebServerRule
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
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
 * Test de integración del [DefaultArticlesRepository] real contra un MockWebServer HTTP: cubre
 * juntos el contrato de red, la deserialización y el mapeo del contrato **valores + excepciones**
 * (éxito devuelve el dato; el error se lanza como excepción, incluido el JSON malformado).
 *
 * **Dobles usados:**
 * - [serverRule] → STUB a nivel HTTP: `enqueueJson` fija las respuestas enlatadas. En
 *   `getArticles with custom limit and offset` se lee además `server.takeRequest()`, que es uso
 *   SPY: registra la request real para assertearla.
 * - `mockk<ArticleDao>(relaxed = true)` en `createRepository()` → DUMMY: rellena el segundo
 *   parámetro del constructor y ningún test de este archivo lo usa (`getCachedArticle` no se
 *   ejercita acá).
 * - NO son dobles: el `api` (Retrofit real contra el MockWebServer) ni [TestJson].
 *
 * **Estilos (state/behavioral):** casi todo STATE (el valor devuelto o la excepción lanzada);
 * BEHAVIORAL en `getArticles with custom limit and offset`, que verifica los query params de la
 * request vía el spy.
 */
class ArticlesRepositoryTest {

    // DOBLE — STUB (nivel HTTP): respuestas enlatadas que consume el Retrofit real. El test de
    // limit/offset además lo usa como SPY (`takeRequest()` registra la request para assertearla).
    @get:Rule
    val serverRule = MockWebServerRule()

    private fun createRepository(): DefaultArticlesRepository {
        // api REAL (no doble): Retrofit + serialización de producción, apuntando al stub HTTP.
        val api = Retrofit.Builder()
            .baseUrl(serverRule.baseUrl())
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }
                    .asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(ApiService::class.java)
        // DOBLE — DUMMY: se pasa al constructor y ningún test de este archivo lo usa (solo
        // `getCachedArticle` lo tocaría y no se ejercita); relaxed para que una llamada accidental
        // no rompa el test.
        val dao = mockk<ArticleDao>(relaxed = true)
        return DefaultArticlesRepository(api, dao)
    }

    /** Ejecuta [block] esperando que lance: devuelve la excepción (falla el test si no lanzó). */
    private suspend fun expectFailure(block: suspend () -> Unit): Throwable {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return e
        }
        throw AssertionError("Expected an exception, but the call succeeded")
    }

    private suspend fun expectHttpError(block: suspend () -> Unit): HttpException {
        val error = expectFailure(block)
        assertTrue("Expected HttpException, got $error", error is HttpException)
        return error as HttpException
    }

    // STATE — el STUB devuelve un 200 y se assertea el valor resultante (lista mapeada a dominio).
    // La verificación es sobre el estado, no sobre la llamada al api.
    @Test
    fun `getArticles on 200 returns articles`() = runTest {
        serverRule.enqueueJson(200, TestJson.ARTICLE_RESPONSE)

        val articles = createRepository().getArticles()

        assertEquals(2, articles.size)
        assertEquals("Article 1", articles[0].title)
        assertEquals("Article 2", articles[1].title)
    }

    // STATE — el contrato de error del repo: ante 500 lanza la HttpException del código.
    // Se assertea el outcome (excepción), sin verificar interacciones.
    @Test
    fun `getArticles on 500 throws HttpException with the code`() = runTest {
        serverRule.enqueueJson(500, """{"error":"Internal error"}""")

        val error = expectHttpError { createRepository().getArticles() }

        assertEquals(500, error.code())
    }

    // STATE — borde del mismo contrato con 404: se lanza HttpException. Sin dobles extra ni
    // verificación de la request.
    @Test
    fun `getArticles on 404 throws HttpException`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        expectHttpError { createRepository().getArticles() }
    }

    // STATE — endpoint single: el STUB aporta el body y se assertea el artículo devuelto;
    // aserción de estado, no de interacción.
    @Test
    fun `getArticle on 200 returns the article`() = runTest {
        serverRule.enqueueJson(200, TestJson.SINGLE_ARTICLE)

        val article = createRepository().getArticle(1)

        assertEquals("Article Detail", article.title)
    }

    // STATE — borde de error del endpoint single: 404 → HttpException. Mismo estilo state que el
    // resto de la matriz de códigos.
    @Test
    fun `getArticle on 404 throws HttpException`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        expectHttpError { createRepository().getArticle(999) }
    }

    // STATE — el STUB sirve un body inválido y el contrato propaga la excepción de serialización
    // (ya no se convierte en un fallo enlatado). Se assertea el tipo de excepción lanzada.
    @Test
    fun `getArticles on malformed JSON throws SerializationException`() = runTest {
        serverRule.enqueueJson(200, """{broken json}""")

        val error = expectFailure { createRepository().getArticles() }

        assertTrue("Expected SerializationException, got $error", error is SerializationException)
    }

    // STATE — search devuelve el resultado filtrado enlatado; se assertea el valor devuelto
    // (éxito con 1 item y su título). El STUB de red solo aporta el body.
    @Test
    fun `searchArticles on 200 returns filtered results`() = runTest {
        serverRule.enqueueJson(200, TestJson.SEARCH_RESPONSE)

        val articles = createRepository().searchArticles("Article 1")

        assertEquals(1, articles.size)
        assertEquals("Article 1", articles[0].title)
    }

    // BEHAVIORAL — el contrato bajo test es *cómo* se pide la página: se verifica la request
    // registrada por el spy (`takeRequest()`) — query params limit/offset — y no el resultado de
    // la llamada.
    @Test
    fun `getArticles with custom limit and offset`() = runTest {
        serverRule.enqueueJson(200, TestJson.ARTICLE_RESPONSE)

        createRepository().getArticles(limit = 10, offset = 5)

        val request = serverRule.server.takeRequest()
        assertEquals("10", request.requestUrl?.queryParameter("limit"))
        assertEquals("5", request.requestUrl?.queryParameter("offset"))
    }

    // STATE — borde de error del search: 500 → HttpException lanzada. Verificación de estado del
    // outcome, sin asserciones de interacción.
    @Test
    fun `searchArticles on 500 throws HttpException`() = runTest {
        serverRule.enqueueJson(500, """{"error":"Server error"}""")

        expectHttpError { createRepository().searchArticles("test") }
    }

    // STATE — caso borde del search: respuesta vacía enlatada → lista vacía devuelta. Se assertea
    // el valor resultante, no la query enviada.
    @Test
    fun `searchArticles with empty results returns empty list`() = runTest {
        serverRule.enqueueJson(200, TestJson.EMPTY_RESPONSE)

        val articles = createRepository().searchArticles("nonexistent")

        assertEquals(0, articles.size)
    }
}
