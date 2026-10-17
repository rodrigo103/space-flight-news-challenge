package com.example.myandroidapp.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.RemoteMediator.MediatorResult.Success
import com.example.myandroidapp.data.mappers.toEntity
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.data.remote.configureApiClient
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.model.ArticleResponse
import com.example.myandroidapp.test.FakeArticleDao
import com.example.myandroidapp.test.RecordedRequests
import com.example.myandroidapp.test.testApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests de [ArticleRemoteMediator] con dobles escritos a mano: MockK no corre en Kotlin/Native,
 * así que en `commonTest` no hay framework de mocking y cada doble se implementa a mano
 * (ver `test/Fakes.kt`).
 *
 * Dobles usados:
 * - `FakeArticleDao` → FAKE (implementación simplificada pero funcional: `MutableMap` real y expone `inserted`).
 * - `testApiService(...)` → STUB (el `MockEngine` de Ktor responde HTTP enlatado vía `respond`).
 * - `failingApi()` → STUB (MockEngine configurado para tirar `IOException`: fallo fijo).
 * - `RecordedRequests` → SPY (registra las URLs/llamadas para assertearlas después).
 * - `article()/articlesJson()/emptyState()/stateWith()/toEntity()` → datos de prueba (fixtures, no dobles).
 *
 * Estilos (state/behavioral):
 * - STATE: `refresh on a partial page…`, `http error…` y `network failure is swallowed…`.
 * - STATE + BEHAVIORAL: `refresh on a full page…` (estado + URL del request) y `prepend succeeds…`
 *   (estado + verificación de que NO se interactúa: sin red ni dao).
 * - BEHAVIORAL: `append uses the number of loaded items as offset` (+ estado del resultado),
 *   `append with an empty state…` y `search query is sent…`.
 */
@OptIn(ExperimentalPagingApi::class)
class ArticleRemoteMediatorTest {

    private val pageSize = 20

    // Dato de prueba (fixture, no doble): Article de dominio con ids predecibles.
    private fun article(id: Int) = Article(
        id = id,
        title = "Article $id",
        summary = "Summary $id",
        url = "https://example.com/article$id",
        publishedAt = "2026-01-01T00:00:00Z",
    )

    // Dato de prueba (fixture): la respuesta que el MockEngine devuelve enlatada, ya serializada.
    private fun articlesJson(count: Int): String = Json.encodeToString(
        ArticleResponse(count = count, results = (1..count).map { article(it) }),
    )

    // STUB — MockEngine configurado para tirar siempre `IOException`: el fallo fijo que consume el SUT.
    private fun failingApi(): ApiService {
        val engine = MockEngine { throw IOException("simulated network failure") }
        return ApiService(HttpClient(engine) { configureApiClient() })
    }

    // Dato de prueba (fixture, no doble): PagingState inicial, sin páginas cargadas.
    private fun emptyState() = PagingState<Int, ArticleEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = pageSize),
        leadingPlaceholderCount = 0,
    )

    // Dato de prueba (fixture, no doble): PagingState con una página ya cargada.
    private fun stateWith(entities: List<ArticleEntity>) = PagingState<Int, ArticleEntity>(
        pages = listOf(PagingSource.LoadResult.Page(entities, prevKey = null, nextKey = null)),
        anchorPosition = null,
        config = PagingConfig(pageSize = pageSize),
        leadingPlaceholderCount = 0,
    )

    // STATE + BEHAVIORAL — el estado del resultado y del fake (Success, paginación abierta,
    // items insertados) + la interacción clave: la URL del request lleva limit/offset correctos.
    @Test
    fun `refresh on a full page inserts items and keeps pagination open`() = runTest {
        // SPY — registra las URLs de las requests para assertear los query params después.
        val recorded = RecordedRequests()
        // FAKE — implementación simplificada pero funcional del DAO (`MutableMap` real + `inserted`).
        val dao = FakeArticleDao()
        // STUB — MockEngine de Ktor: respuestas HTTP enlatadas vía `respond`.
        val api = testApiService(body = articlesJson(pageSize), recorded = recorded)

        val result = ArticleRemoteMediator(api, dao).load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertFalse((result as Success).endOfPaginationReached)
        assertEquals(pageSize, dao.inserted.size)
        assertEquals("Article 1", dao.inserted.first { it.id == 1 }.title)
        val url = recorded.urls.first()
        assertContains(url, "limit=$pageSize")
        assertContains(url, "offset=0")
    }

    // STATE — solo el estado resultante: `endOfPaginationReached` en true y 2 items persistidos
    // en el fake. No verifica interacciones.
    @Test
    fun `refresh on a partial page marks end of pagination`() = runTest {
        val dao = FakeArticleDao()
        val api = testApiService(body = articlesJson(2))

        val result = ArticleRemoteMediator(api, dao).load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertTrue((result as Success).endOfPaginationReached)
        assertEquals(2, dao.inserted.size)
    }

    // BEHAVIORAL — verifica la interacción: la URL usa como offset la cantidad de items ya
    // cargados. Incluye un assert de estado del resultado (`Success`).
    @Test
    fun `append uses the number of loaded items as offset`() = runTest {
        val recorded = RecordedRequests()
        val dao = FakeArticleDao()
        val api = testApiService(body = articlesJson(2), recorded = recorded)
        // Dato de prueba: `toEntity()` (mapper real de producción) construye las entities cargadas.
        val loaded = (1..pageSize).map { article(it).toEntity() }

        val result = ArticleRemoteMediator(api, dao).load(LoadType.APPEND, stateWith(loaded))

        assertTrue(result is Success)
        assertContains(recorded.urls.first(), "offset=$pageSize")
    }

    // BEHAVIORAL — sin state verification: solo la interacción, con el estado vacío el offset
    // de la URL cae a 0.
    @Test
    fun `append with an empty state falls back to offset zero`() = runTest {
        val recorded = RecordedRequests()
        val dao = FakeArticleDao()
        val api = testApiService(body = articlesJson(2), recorded = recorded)

        ArticleRemoteMediator(api, dao).load(LoadType.APPEND, emptyState())

        assertContains(recorded.urls.first(), "offset=0")
    }

    // STATE + BEHAVIORAL — estado del resultado (Success terminal) + la interacción "negativa":
    // PREPEND no debe tocar la red ni el dao (el spy y el fake quedan vacíos).
    @Test
    fun `prepend succeeds without touching the network or the dao`() = runTest {
        val recorded = RecordedRequests()
        val dao = FakeArticleDao()
        val api = testApiService(recorded = recorded)

        val result = ArticleRemoteMediator(api, dao).load(LoadType.PREPEND, emptyState())

        assertTrue(result is Success)
        assertTrue((result as Success).endOfPaginationReached)
        assertEquals(0, recorded.urls.size)
        assertEquals(0, dao.inserted.size)
    }

    // BEHAVIORAL — verifica la interacción: el query param `search` viaja en la request.
    @Test
    fun `search query is sent as request parameter`() = runTest {
        val recorded = RecordedRequests()
        val api = testApiService(body = articlesJson(1), recorded = recorded)

        ArticleRemoteMediator(api, FakeArticleDao(), searchQuery = "mars")
            .load(LoadType.REFRESH, emptyState())

        assertContains(recorded.urls.first(), "search=mars")
    }

    // STATE — solo el resultado de la mediación: un HTTP 500 se mapea a MediatorResult.Error.
    @Test
    fun `http error returns MediatorResult Error`() = runTest {
        val api = testApiService(
            status = HttpStatusCode.InternalServerError,
            body = """{"error":"boom"}""",
        )

        val result = ArticleRemoteMediator(api, FakeArticleDao()).load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
    }

    // STATE — el fallo de red (`IOException`) se traga como Success no terminal: importa el
    // estado del resultado, no la interacción. `failingApi()` es el STUB del fallo fijo.
    @Test
    fun `network failure is swallowed as non-terminal success`() = runTest {
        val result = ArticleRemoteMediator(failingApi(), FakeArticleDao())
            .load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertFalse((result as Success).endOfPaginationReached)
    }
}
