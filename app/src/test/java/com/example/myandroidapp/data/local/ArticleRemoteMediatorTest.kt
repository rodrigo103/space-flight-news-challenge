package com.example.myandroidapp.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.RemoteMediator.MediatorResult.Success
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.myandroidapp.data.mappers.toEntity
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.model.ArticleResponse
import com.example.myandroidapp.rules.MockWebServerRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Test de integración del [ArticleRemoteMediator]: corre contra un MockWebServer HTTP real
 * (stub de respuestas) y una DB Room in-memory real, así que cubre juntos el contrato de red,
 * la deserialización, el mapeo a entity y la persistencia.
 *
 * **Dobles usados:**
 * - [serverRule] → STUB a nivel HTTP: `enqueueJson` fija respuestas enlatadas que consume
 *   Retrofit. Además `server.takeRequest()` es uso SPY: registra la request real para assertearla.
 * - `mockk<ApiService>()` en `network failures` → STUB puro: `coEvery { … } throws` fija el
 *   fallo; no se verifica ninguna interacción.
 * - NO son dobles: `api` (Retrofit real apuntando al MockWebServer) ni `db`/`dao` (Room real).
 * - `article()/articlesJson()/emptyState()/stateWith()` → datos de prueba, no dobles.
 *
 * **Estilos (state/behavioral):** mixto. La aserción primaria es STATE sobre el
 * [RemoteMediator.MediatorResult] y la DB; los tests de paginación/search agregan BEHAVIORAL
 * sobre la URL de la request (el MockWebServer oficia de spy).
 */
@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class ArticleRemoteMediatorTest {

    // DOBLE — STUB (nivel HTTP): entrega respuestas enlatadas y fijas; el SUT (Retrofit) las
    // consume sin saber que están scripteadas. Su `server.takeRequest()` se usa además como SPY:
    // registra las requests reales para poder assertear sus URLs.
    @get:Rule
    val serverRule = MockWebServerRule()

    private lateinit var db: AppDatabase
    private lateinit var dao: ArticleDao
    private lateinit var api: ApiService

    private val pageSize = 20

    @Before
    fun setUp() {
        // Componentes REALES (no dobles): misma DB/DAO que en producción; el colaborador remoto
        // se reemplaza por el stub HTTP del MockWebServer (`serverRule.baseUrl()`).
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.articleDao()
        // Retrofit real: sólo cambia la baseUrl hacia el MockWebServer; cliente HTTP,
        // serialización y contrato de ApiService son los de producción.
        api = Retrofit.Builder()
            .baseUrl(serverRule.baseUrl())
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }
                    .asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(ApiService::class.java)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Datos de prueba (no dobles): el stub HTTP es la respuesta enlatada que se encola con
    // `enqueueJson`; estos builders solo arman el contenido del body.
    private fun article(id: Int) = Article(
        id = id,
        title = "Article $id",
        summary = "Summary $id",
        url = "https://example.com/article$id",
        publishedAt = "2026-01-01T00:00:00Z",
    )

    private fun articlesJson(count: Int): String = Json.encodeToString(
        ArticleResponse(count = count, results = (1..count).map { article(it) }),
    )

    private fun emptyState() = PagingState<Int, ArticleEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = pageSize),
        leadingPlaceholderCount = 0,
    )

    private fun stateWith(entities: List<ArticleEntity>) = PagingState<Int, ArticleEntity>(
        pages = listOf(PagingSource.LoadResult.Page(entities, prevKey = null, nextKey = null)),
        anchorPosition = null,
        config = PagingConfig(pageSize = pageSize),
        leadingPlaceholderCount = 0,
    )

    private fun mediator(search: String? = null) = ArticleRemoteMediator(api, dao, search)

    // STATE + BEHAVIORAL — el STUB del server devuelve una página completa; se assertea el
    // estado (Success sin fin de paginación + filas persistidas) y ADEMÁS la interacción: la
    // URL pedida lleva limit/offset correctos (assert sobre la request, vía spy).
    @Test
    fun `refresh on a full page inserts items and keeps pagination open`() = runTest {
        serverRule.enqueueJson(200, articlesJson(pageSize))

        val result = mediator().load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertFalse((result as Success).endOfPaginationReached)
        assertNotNull(dao.getById(1))
        assertNotNull(dao.getById(pageSize))
        val path = serverRule.server.takeRequest().path!!
        assertTrue(path.contains("limit=$pageSize"))
        assertTrue(path.contains("offset=0"))
    }

    // STATE — una página incompleta debe marcar el fin de la paginación; solo se assertean el
    // MediatorResult y la fila persistida, sin inspeccionar la request.
    @Test
    fun `refresh on a partial page marks end of pagination`() = runTest {
        serverRule.enqueueJson(200, articlesJson(2))

        val result = mediator().load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertTrue((result as Success).endOfPaginationReached)
        assertNotNull(dao.getById(2))
    }

    // BEHAVIORAL (+ STATE) — el contrato bajo test es *cómo* se pide la página: el offset debe
    // ser la cantidad de items ya cargados. La verificación principal es sobre la request; el
    // `Success` es el estado complementario.
    @Test
    fun `append uses the number of loaded items as offset`() = runTest {
        serverRule.enqueueJson(200, articlesJson(2))
        val loaded = (1..pageSize).map { article(it).toEntity() }

        val result = mediator().load(LoadType.APPEND, stateWith(loaded))

        assertTrue(result is Success)
        val path = serverRule.server.takeRequest().path!!
        assertTrue(path.contains("offset=$pageSize"))
    }

    // BEHAVIORAL — caso borde del mismo contrato: sin items cargados, el offset enviado es 0.
    // No se assertea el resultado, solo la URL de la request registrada por el spy.
    @Test
    fun `append with an empty state falls back to offset zero`() = runTest {
        serverRule.enqueueJson(200, articlesJson(2))

        mediator().load(LoadType.APPEND, emptyState())

        val path = serverRule.server.takeRequest().path!!
        assertTrue(path.contains("offset=0"))
    }

    // BEHAVIORAL + STATE — verificación negativa de interacción: PREPEND no debe tocar la red
    // (`requestCount == 0`, leído del spy). El Success/endOfPagination es el estado esperado.
    @Test
    fun `prepend succeeds without touching the network`() = runTest {
        val result = mediator().load(LoadType.PREPEND, emptyState())

        assertTrue(result is Success)
        assertTrue((result as Success).endOfPaginationReached)
        assertEquals(0, serverRule.server.requestCount)
    }

    // BEHAVIORAL — el search se propaga como query param: se verifica la URL de la request
    // registrada (spy), no el resultado de la carga.
    @Test
    fun `search query is sent as request parameter`() = runTest {
        serverRule.enqueueJson(200, articlesJson(1))

        mediator(search = "mars").load(LoadType.REFRESH, emptyState())

        val path = serverRule.server.takeRequest().path!!
        assertTrue(path.contains("search=mars"))
    }

    // STATE — ante un 500 (respuesta enlatada del stub) el resultado esperado es
    // MediatorResult.Error: se assertea el estado de salida, sin verificar la interacción.
    @Test
    fun `http error returns MediatorResult Error`() = runTest {
        serverRule.enqueueJson(500, "{}")

        val result = mediator().load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
    }

    // STATE — usa la lista de fallos como dato de prueba y un STUB de ApiService para simular
    // cada excepción de red (coEvery … throws); se assertea el estado resultante: Success no
    // terminal, la red se degrada sin romper la paginación.
    @Test
    fun `network failures are swallowed as non-terminal success`() = runTest {
        val exceptions = listOf(
            UnknownHostException("offline"),
            ConnectException("refused"),
            SocketTimeoutException("timeout"),
        )
        for (exception in exceptions) {
            // STUB — mockk usado SOLO para devolver/fijar un valor (acá: lanzar). Se vuelve MOCK
            // recién si el test verificara sus interacciones, cosa que este test no hace.
            val failingApi = mockk<ApiService>()
            coEvery { failingApi.getArticles(any(), any(), any()) } throws exception

            val result = ArticleRemoteMediator(failingApi, dao).load(LoadType.REFRESH, emptyState())

            assertTrue("Expected Success for $exception", result is Success)
            assertFalse((result as Success).endOfPaginationReached)
        }
    }
}
