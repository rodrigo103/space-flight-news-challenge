package com.example.myandroidapp.domain.usecase

import com.example.myandroidapp.TestArticleData
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test del [GetArticleUseCase] en JVM puro: el repositorio (colaborador) se stubbea con MockK
 * y el timeout/degradación a caché se ejercita con el virtual time de [runTest].
 *
 * **Dobles usados:**
 * - `repository = mockk<ArticlesRepository>()` + `coEvery { … } returns` / `coAnswers` → STUB:
 *   solo aporta respuestas enlatadas (éxito, fallo, timeout); ningún test verifica sus
 *   interacciones, así que nunca pasa a MOCK.
 * - `mockk<Article>()` dentro de los `coAnswers` de timeout → placeholder DUMMY: rellena la
 *   respuesta enlatada, pero el timeout de 30 s cancela el `delay(60_000)` y nunca llega a usarse.
 *
 * **Estilos (state/behavioral):** 100% STATE — cada test assertea el `Result` resultante (éxito
 * con el artículo, fallback a caché, failure/timeout), sin `coVerify`.
 */
class GetArticleUseCaseTest {

    // DOBLE — STUB: `mockk` configurado en cada test con `coEvery { … } returns/coAnswers`
    // (respuestas enlatadas). Nunca se lo verifica con `coVerify`, por lo que no actúa como MOCK.
    private val repository = mockk<ArticlesRepository>()

    private fun createUseCase() = GetArticleUseCase(repository)

    // STATE — el STUB enlata un éxito y se assertea el `Result` con el artículo: la verificación
    // es sobre el estado de salida del use case.
    @Test
    fun `invoke returns article on network success`() = runTest {
        val article = TestArticleData.articleDetail
        coEvery { repository.getArticle(1) } returns Result.success(article)

        val result = createUseCase()(1)

        assertTrue(result.isSuccess)
        assertEquals(article, result.getOrThrow())
    }

    // STATE — regla de negocio: si la red falla y hay caché, el resultado es success con el
    // artículo cacheado. Se assertea el estado resultante, no las llamadas al repositorio.
    @Test
    fun `invoke returns article from cache on network failure`() = runTest {
        val cached = TestArticleData.article1
        coEvery { repository.getArticle(1) } returns Result.failure(Exception("Network error"))
        coEvery { repository.getCachedArticle(1) } returns cached

        val result = createUseCase()(1)

        assertTrue(result.isSuccess)
        assertEquals(cached, result.getOrThrow())
    }

    // STATE — borde sin caché: el failure de red se propaga como failure con el mensaje original;
    // aserción sobre el `Result`, sin verificar interacciones.
    @Test
    fun `invoke returns failure when network fails and no cache`() = runTest {
        coEvery { repository.getArticle(1) } returns Result.failure(Exception("Not found"))
        coEvery { repository.getCachedArticle(1) } returns null

        val result = createUseCase()(1)

        assertTrue(result.isFailure)
        assertEquals("Not found", result.exceptionOrNull()?.message)
    }

    // STATE — el STUB simula una red colgada (delay mayor al timeout del use case) y se assertea
    // la degradación: success con el artículo de caché. El resultado del timeout nunca se entrega.
    @Test
    fun `invoke falls back to cache when network times out`() = runTest {
        val cached = TestArticleData.article2
        // STUB — `coAnswers` es una respuesta enlatada con lógica: delay + éxito. El timeout de
        // 30 s del use case cancela el delay de 60 s (virtual time de runTest) y el resultado real
        // es la cancelación.
        coEvery { repository.getArticle(2) } coAnswers {
            delay(60_000)
            // DUMMY/placeholder: nunca se construye (el timeout corta antes); rellena la rama que
            // la lambda del stub exige para tipar `Result<Article>`.
            Result.success(mockk<Article>())
        }
        coEvery { repository.getCachedArticle(2) } returns cached

        val result = createUseCase()(2)

        assertTrue(result.isSuccess)
        assertEquals(cached, result.getOrThrow())
    }

    // STATE — borde sin red ni caché: el timeout se traduce en failure("Request timed out"); la
    // aserción es el mensaje del `Result`, no las llamadas. Mismo placeholder DUMMY que el test
    // anterior dentro del `coAnswers`.
    @Test
    fun `invoke returns timeout error when no network and no cache`() = runTest {
        // STUB — mismo patrón: delay + éxito enlatado que el timeout deja sin efecto.
        coEvery { repository.getArticle(3) } coAnswers {
            delay(60_000)
            Result.success(mockk<Article>())
        }
        coEvery { repository.getCachedArticle(3) } returns null

        val result = createUseCase()(3)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message == "Request timed out")
    }

    // STATE — regla de precedencia: con red y caché disponibles gana la red; se assertea el
    // artículo resultante (el de red), no la secuencia de llamadas que llevó a elegirlo.
    @Test
    fun `invoke prefers network over cache when both available`() = runTest {
        val networkArticle = TestArticleData.articleDetail
        val cachedArticle = TestArticleData.article1
        coEvery { repository.getArticle(1) } returns Result.success(networkArticle)
        coEvery { repository.getCachedArticle(1) } returns cachedArticle

        val result = createUseCase()(1)

        assertTrue(result.isSuccess)
        assertEquals(networkArticle, result.getOrThrow())
    }
}
