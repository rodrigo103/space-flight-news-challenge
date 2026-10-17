package com.example.myandroidapp.data

import com.example.myandroidapp.data.remote.ApiException
import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.data.repository.DefaultArticlesRepository
import com.example.myandroidapp.test.FakeArticleDao
import com.example.myandroidapp.test.RecordedRequests
import com.example.myandroidapp.test.TestJson
import com.example.myandroidapp.test.testApiService
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests de [DefaultArticlesRepository]: el repositorio real montado sobre un ApiService con
 * MockEngine enlatado y un DAO fake — solo las hojas están dobladas, la lógica de la capa corre real.
 *
 * Dobles usados:
 * - `testApiService(...)` dentro de `repository()` → STUB: MockEngine con status/body fijos.
 * - `FakeArticleDao` → FAKE: DAO en memoria funcional (`MutableMap` real).
 * - `RecordedRequests` → SPY: registra las URLs de las requests para assertear los query params.
 * - `TestJson.*` → datos de prueba (fixtures JSON, no dobles).
 *
 * Estilos (state/behavioral):
 * - STATE: success/failure, mapeo de errores y caché (asserts sobre el `ApiResult` resultante).
 * - BEHAVIORAL: `getArticles with custom limit and offset hits the right query` y
 *   `searchArticles sends the query as search param` (verifican la URL del request).
 */
class ArticlesRepositoryTest {

    // Composición de dobles por default: STUB (MockEngine enlatado), FAKE (DAO en memoria) y, si
    // el test lo pide, SPY (RecordedRequests). Cada test overridea status/body/dao según el caso.
    private fun repository(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = TestJson.ARTICLE_RESPONSE,
        dao: FakeArticleDao = FakeArticleDao(),
        recorded: RecordedRequests? = null,
    ) = DefaultArticlesRepository(testApiService(status, body, recorded), dao)

    // STATE — camino feliz del listado: 200 enlatado (STUB) ⇒ Success con los articles mapeados
    // a dominio.
    @Test
    fun `getArticles on 200 returns success`() = runTest {
        val result = repository().getArticles()

        val success = assertIs<ApiResult.Success<List<com.example.myandroidapp.domain.model.Article>>>(result)
        assertEquals(2, success.data.size)
        assertEquals("Article 1", success.data[0].title)
        assertEquals("Article 2", success.data[1].title)
    }

    // STATE — mapeo de error: 500 enlatado (STUB) ⇒ Failure envolviendo ApiException.ServerError.
    @Test
    fun `getArticles on 500 returns failure with ServerError`() = runTest {
        val result = repository(
            status = HttpStatusCode.InternalServerError,
            body = """{"error":"Internal error"}""",
        ).getArticles()

        val failure = assertIs<ApiResult.Failure>(result)
        val error = assertIs<ApiException.ServerError>(failure.error)
        assertEquals(500, error.code)
    }

    // STATE — 404 enlatado (STUB) ⇒ Failure con NotFound adentro.
    @Test
    fun `getArticles on 404 returns failure with NotFound`() = runTest {
        val result = repository(
            status = HttpStatusCode.NotFound,
            body = """{"error":"Not found"}""",
        ).getArticles()

        val failure = assertIs<ApiResult.Failure>(result)
        assertIs<ApiException.NotFound>(failure.error)
    }

    // STATE — detalle OK: Success con el article parseado del body enlatado (STUB).
    @Test
    fun `getArticle on 200 returns success`() = runTest {
        val result = repository(body = TestJson.SINGLE_ARTICLE).getArticle(1)

        val success = assertIs<ApiResult.Success<com.example.myandroidapp.domain.model.Article>>(result)
        assertEquals("Article Detail", success.data.title)
    }

    // STATE — el detalle con 404 ⇒ Failure (mismo mapeo que en el listado).
    @Test
    fun `getArticle on 404 returns failure`() = runTest {
        val result = repository(
            status = HttpStatusCode.NotFound,
            body = """{"error":"Not found"}""",
        ).getArticle(999)

        assertIs<ApiResult.Failure>(result)
    }

    // STATE — JSON inválido ⇒ Failure: `apiRunCatching` envuelve el error de deserialización,
    // no lo propaga.
    @Test
    fun `getArticles on malformed JSON returns failure`() = runTest {
        val result = repository(body = """{broken json}""").getArticles()

        assertIs<ApiResult.Failure>(result)
    }

    // STATE — búsqueda: fixture SEARCH_RESPONSE (STUB) ⇒ Success con el resultado filtrado.
    @Test
    fun `searchArticles on 200 returns filtered results`() = runTest {
        val result = repository(body = TestJson.SEARCH_RESPONSE).searchArticles("Article 1")

        val success = assertIs<ApiResult.Success<List<com.example.myandroidapp.domain.model.Article>>>(result)
        assertEquals(1, success.data.size)
        assertEquals("Article 1", success.data[0].title)
    }

    // STATE — búsqueda con 500 (STUB) ⇒ Failure.
    @Test
    fun `searchArticles on 500 returns failure`() = runTest {
        val result = repository(
            status = HttpStatusCode.InternalServerError,
            body = """{"error":"Server error"}""",
        ).searchArticles("test")

        assertIs<ApiResult.Failure>(result)
    }

    // STATE — lista vacía: es un Success válido (no un error), con `data` vacía.
    @Test
    fun `searchArticles with empty results returns empty list`() = runTest {
        val result = repository(body = TestJson.EMPTY_RESPONSE).searchArticles("nonexistent")

        val success = assertIs<ApiResult.Success<List<com.example.myandroidapp.domain.model.Article>>>(result)
        assertTrue(success.data.isEmpty())
    }

    // BEHAVIORAL — verifica la interacción: limit/offset viajan como query params en la URL
    // (leída del SPY). Sin asserts de estado.
    @Test
    fun `getArticles with custom limit and offset hits the right query`() = runTest {
        // SPY — registra las URLs de las requests para assertear los query params.
        val recorded = RecordedRequests()

        repository(recorded = recorded).getArticles(limit = 10, offset = 5)

        val url = recorded.urls.single()
        assertContains(url, "limit=10")
        assertContains(url, "offset=5")
    }

    // BEHAVIORAL — la query de búsqueda viaja como param `search=` en la URL (SPY).
    @Test
    fun `searchArticles sends the query as search param`() = runTest {
        val recorded = RecordedRequests()

        repository(body = TestJson.SEARCH_RESPONSE, recorded = recorded).searchArticles("mars")

        assertContains(recorded.urls.single(), "search=mars")
    }

    // STATE — el FAKE funciona de verdad: se inserta una entity en su `MutableMap` y
    // `getCachedArticle` la devuelve mapeada a dominio (mapper real de producción).
    @Test
    fun `getCachedArticle returns the entity mapped to domain`() = runTest {
        // FAKE — DAO en memoria: `insert` persiste y `getById` lee.
        val dao = FakeArticleDao()
        val repo = repository(dao = dao)
        dao.insert(
            com.example.myandroidapp.data.local.ArticleEntity(
                id = 7,
                title = "Cached",
                summary = "From Room",
                imageUrl = null,
                newsSite = "NASA",
                publishedAt = "2026-01-01",
                url = "https://example.com/7",
            ),
        )

        val article = repo.getCachedArticle(7)

        assertEquals("Cached", article?.title)
        assertEquals("From Room", article?.summary)
    }

    // STATE — id ausente en el FAKE ⇒ null.
    @Test
    fun `getCachedArticle returns null when absent`() = runTest {
        assertNull(repository().getCachedArticle(404))
    }
}
