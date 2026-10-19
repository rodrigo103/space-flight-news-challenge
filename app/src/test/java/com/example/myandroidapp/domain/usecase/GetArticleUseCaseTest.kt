package com.example.myandroidapp.domain.usecase

import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test del [GetArticleUseCase] en JVM puro: el repositorio (colaborador) se stubbea con MockK
 * y el timeout/degradación a caché se ejercita con el virtual time de [runTest].
 *
 * El use case expone el contrato **valor + excepción**: el camino feliz devuelve el `Article` y
 * los fallos se assertean como excepciones lanzadas (ya no hay `Result`/`ApiResult`).
 *
 * **Dobles usados:**
 * - `repository = mockk<ArticlesRepository>()` + `coEvery { … } returns/throws/coAnswers` → STUB:
 *   solo aporta respuestas enlatadas (valor, excepción, timeout); ningún test verifica sus
 *   interacciones, así que nunca pasa a MOCK.
 * - `mockk<Article>()` dentro de los `coAnswers` de timeout → placeholder DUMMY: rellena el tipo
 *   de retorno de la lambda, pero el timeout de 30 s cancela el `delay(60_000)` y nunca se usa.
 *
 * **Estilos (state/behavioral):** 100% STATE — cada test assertea el valor devuelto o la
 * excepción lanzada, sin `coVerify`.
 */
class GetArticleUseCaseTest {

    // DOBLE — STUB: `mockk` configurado en cada test con `coEvery { … } returns/throws/coAnswers`
    // (respuestas enlatadas). Nunca se lo verifica con `coVerify`, por lo que no actúa como MOCK.
    private val repository = mockk<ArticlesRepository>()

    private fun createUseCase() = GetArticleUseCase(repository)

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

    // STATE — el STUB enlata un éxito y se assertea el artículo devuelto: la verificación es sobre
    // el valor de salida del use case.
    @Test
    fun `invoke returns article on network success`() = runTest {
        val article = TestArticleData.articleDetail
        coEvery { repository.getArticle(1) } returns article

        assertEquals(article, createUseCase()(1))
    }

    // STATE — regla de negocio: si la red falla y hay caché, se devuelve el artículo cacheado. Se
    // assertea el valor resultante, no las llamadas al repositorio.
    @Test
    fun `invoke returns article from cache on network failure`() = runTest {
        val cached = TestArticleData.article1
        coEvery { repository.getArticle(1) } throws Exception("Network error")
        coEvery { repository.getCachedArticle(1) } returns cached

        assertEquals(cached, createUseCase()(1))
    }

    // STATE — borde sin caché: la excepción de red original se relanza tal cual. Aserción sobre la
    // excepción, sin verificar interacciones.
    @Test
    fun `invoke rethrows network failure when no cache`() = runTest {
        coEvery { repository.getArticle(1) } throws Exception("Not found")
        coEvery { repository.getCachedArticle(1) } returns null

        val error = expectFailure { createUseCase()(1) }

        assertEquals("Not found", error.message)
    }

    // STATE — el STUB simula una red colgada (delay mayor al timeout del use case) y se assertea
    // la degradación: se devuelve el artículo de caché. El retorno del timeout nunca se entrega.
    @Test
    fun `invoke falls back to cache when network times out`() = runTest {
        val cached = TestArticleData.article2
        // STUB — `coAnswers` es una respuesta enlatada con lógica: delay + valor. El timeout de
        // 30 s del use case cancela el delay de 60 s (virtual time de runTest).
        coEvery { repository.getArticle(2) } coAnswers {
            delay(60_000)
            // DUMMY/placeholder: nunca se construye (el timeout corta antes); rellena el tipo de
            // retorno que la lambda del stub exige.
            mockk<Article>()
        }
        coEvery { repository.getCachedArticle(2) } returns cached

        assertEquals(cached, createUseCase()(2))
    }

    // STATE — borde sin red ni caché: el timeout se traduce en `TimeoutException("Request timed
    // out")`; la aserción es el mensaje de la excepción, no las llamadas. Mismo placeholder DUMMY
    // que el test anterior dentro del `coAnswers`.
    @Test
    fun `invoke throws timeout when no network and no cache`() = runTest {
        // STUB — mismo patrón: delay + valor enlatado que el timeout deja sin efecto.
        coEvery { repository.getArticle(3) } coAnswers {
            delay(60_000)
            mockk<Article>()
        }
        coEvery { repository.getCachedArticle(3) } returns null

        val error = expectFailure { createUseCase()(3) }

        assertEquals("Request timed out", error.message)
    }

    // STATE — regla de precedencia: con red y caché disponibles gana la red; se assertea el
    // artículo resultante (el de red), no la secuencia de llamadas que llevó a elegirlo.
    @Test
    fun `invoke prefers network over cache when both available`() = runTest {
        val networkArticle = TestArticleData.articleDetail
        coEvery { repository.getArticle(1) } returns networkArticle
        coEvery { repository.getCachedArticle(1) } returns TestArticleData.article1

        assertEquals(networkArticle, createUseCase()(1))
    }
}
