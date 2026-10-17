package com.example.myandroidapp.data.local

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test de integración del [ArticleDao] real contra una base Room in-memory — el patrón
 * recomendado para DAO (nota 28, "Estrategias de testing", §1.4). Corre en JVM con Robolectric,
 * sin emulador.
 *
 * **Dobles usados:** ninguno. El SUT ([ArticleDao]) y su colaborador ([AppDatabase]) son
 * componentes reales generados por Room; los `entity(...)` son datos de prueba, no dobles.
 * La DB in-memory es una base SQLite real que se descarta al cerrar.
 *
 * **Estilos (state/behavioral):** 100% STATE — todos los tests assertean el *resultado* de las
 * queries/paging (filas recuperadas, orden, matches). No hay interacciones que verificar.
 */
@RunWith(RobolectricTestRunner::class)
class ArticleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ArticleDao

    @Before
    fun setUp() {
        // SIN DOBLES — DB Room REAL (in-memory): mismo motor SQLite y mismos DAOs que en
        // producción, descartada al cerrar. El in-memory es el patrón recomendado para DAO.
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        // SUT real: el DAO generado por Room, sin decoradores ni dobles.
        dao = db.articleDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Datos de prueba (no un doble): builder de filas con defaults para que cada test solo
    // destaque los campos que le importan.
    private fun entity(
        id: Int,
        title: String = "Title $id",
        summary: String = "Summary $id",
        publishedAt: String? = null,
    ) = ArticleEntity(
        id = id,
        title = title,
        summary = summary,
        imageUrl = null,
        newsSite = "NASA",
        publishedAt = publishedAt,
        url = "https://example.com/article$id",
    )

    private suspend fun loadAll(source: PagingSource<Int, ArticleEntity>): List<ArticleEntity> {
        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false),
        )
        return (result as PagingSource.LoadResult.Page).data
    }

    // STATE — inserta filas reales y assertea el estado observable: `getById` devuelve lo
    // insertado. La verificación es sobre el resultado de la query, no sobre las llamadas.
    @Test
    fun `insertAll stores articles and getById returns them`() = runTest {
        dao.insertAll(listOf(entity(1), entity(2)))

        assertEquals("Title 1", dao.getById(1)?.title)
        assertEquals("Title 2", dao.getById(2)?.title)
    }

    // STATE — el resultado de la query sobre una DB vacía es `null`; se assertea el estado
    // esperado sin preparar ningún colaborador.
    @Test
    fun `getById returns null for unknown id`() = runTest {
        assertNull(dao.getById(99))
    }

    // STATE — verifica la semántica REPLACE del conflicto observando el valor final de la fila;
    // la aserción es sobre el estado persistido, no sobre cómo Room resolvió el upsert.
    @Test
    fun `insert replaces existing article on conflict`() = runTest {
        dao.insert(entity(1, title = "Old"))
        dao.insertAll(listOf(entity(1, title = "New")))

        assertEquals("New", dao.getById(1)?.title)
    }

    // STATE — assertea el estado resultante tras `clearAll`: el paging source real queda vacío.
    // El helper `loadAll` consulta a Room, no a un fake.
    @Test
    fun `clearAll removes every article`() = runTest {
        dao.insertAll(listOf(entity(1), entity(2)))

        dao.clearAll()

        assertEquals(emptyList<ArticleEntity>(), loadAll(dao.pagingSource()))
    }

    // STATE — assertea el orden que devuelve la query; los datos se insertan desordenados a
    // propósito para validar el ORDER BY de la query y no el orden de inserción.
    @Test
    fun `pagingSource orders by publishedAt descending`() = runTest {
        dao.insertAll(
            listOf(
                entity(1, publishedAt = "2026-01-01"),
                entity(2, publishedAt = "2026-01-03"),
                entity(3, publishedAt = "2026-01-02"),
            ),
        )

        assertEquals(listOf(2, 3, 1), loadAll(dao.pagingSource()).map { it.id })
    }

    // STATE — assertea el conjunto resultante del paging source en dos capitalizaciones: cubre
    // el contrato de la query (title/summary, case-insensitive), sin verificar interacciones.
    @Test
    fun `searchPagingSource matches title or summary ignoring case`() = runTest {
        dao.insertAll(
            listOf(
                entity(1, title = "Mars mission", summary = "red planet", publishedAt = "2026-01-01"),
                entity(2, title = "Jupiter flyby", summary = "Mars orbiter", publishedAt = "2026-01-02"),
                entity(3, title = "Moon base", summary = "lunar habitat", publishedAt = "2026-01-03"),
            ),
        )

        assertEquals(listOf(2, 1), loadAll(dao.searchPagingSource("mars")).map { it.id })
        assertEquals(listOf(2, 1), loadAll(dao.searchPagingSource("MARS")).map { it.id })
    }

    // STATE — caso borde del resultado de la query: sin matches, el paging source devuelve lista
    // vacía. Mismo estilo state que el resto del archivo.
    @Test
    fun `searchPagingSource returns empty when nothing matches`() = runTest {
        dao.insertAll(listOf(entity(1, title = "Mars mission")))

        assertEquals(emptyList<ArticleEntity>(), loadAll(dao.searchPagingSource("venus")))
    }
}
