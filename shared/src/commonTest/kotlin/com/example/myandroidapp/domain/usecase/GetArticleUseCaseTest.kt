package com.example.myandroidapp.domain.usecase

import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.test.FakeArticlesRepository
import com.example.myandroidapp.test.TestArticleData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests de [GetArticleUseCase]: la política de fallback red → caché → error/timeout, con un
 * repositorio fake configurable. Todos los tests son de estado: assertean el `ApiResult` final.
 *
 * Dobles usados:
 * - `FakeArticlesRepository` → FAKE: implementación simplificada pero funcional; los campos
 *   (`articleResult`, `cachedArticle`, `getArticleDelayMs`) se configuran antes de invocar, y su
 *   `delay` real corre en virtual time de `runTest`.
 * - `TestArticleData` → datos de prueba (fixture, no doble).
 *
 * Estilos (state/behavioral):
 * - STATE: los 6 tests (nadie verifica interacciones; solo importa el resultado).
 */
class GetArticleUseCaseTest {

    // FAKE — repositorio configurable en memoria: cada test setea sus campos antes de invocar.
    private val repository = FakeArticlesRepository()
    private val useCase = GetArticleUseCase(repository)

    // STATE — camino feliz: la red responde (fake) ⇒ Success con el article de red.
    @Test
    fun `invoke returns article on network success`() = runTest {
        val article = TestArticleData.articleDetail
        repository.articleResult = ApiResult.Success(article)

        val result = useCase(1)

        assertEquals(article, assertIs<ApiResult.Success<*>>(result).data)
    }

    // STATE — fallback: la red falla y hay caché ⇒ Success con el article cacheado.
    @Test
    fun `invoke returns article from cache on network failure`() = runTest {
        val cached = TestArticleData.article1
        repository.articleResult = ApiResult.Failure(IllegalStateException("Network error"))
        repository.cachedArticle = cached

        val result = useCase(1)

        assertEquals(cached, assertIs<ApiResult.Success<*>>(result).data)
    }

    // STATE — sin red ni caché ⇒ Failure con el error original de la red.
    @Test
    fun `invoke returns failure when network fails and no cache`() = runTest {
        repository.articleResult = ApiResult.Failure(IllegalStateException("Not found"))
        repository.cachedArticle = null

        val result = useCase(1)

        assertEquals("Not found", assertIs<ApiResult.Failure>(result).error.message)
    }

    // STATE — timeout en virtual time: el fake demora 60s y el use case corta a los 30s
    // (`withTimeoutOrNull`) ⇒ cae a la caché.
    @Test
    fun `invoke falls back to cache when network times out`() = runTest {
        val cached = TestArticleData.article2
        repository.getArticleDelayMs = 60_000
        repository.cachedArticle = cached

        val result = useCase(2)

        assertEquals(cached, assertIs<ApiResult.Success<*>>(result).data)
    }

    // STATE — timeout sin caché ⇒ Failure("Request timed out").
    @Test
    fun `invoke returns timeout error when no network and no cache`() = runTest {
        repository.getArticleDelayMs = 60_000
        repository.cachedArticle = null

        val result = useCase(3)

        assertEquals("Request timed out", assertIs<ApiResult.Failure>(result).error.message)
    }

    // STATE — precedencia: con red y caché disponibles gana el resultado de red.
    @Test
    fun `invoke prefers network over cache when both available`() = runTest {
        repository.articleResult = ApiResult.Success(TestArticleData.articleDetail)
        repository.cachedArticle = TestArticleData.article1

        val result = useCase(1)

        assertEquals(TestArticleData.articleDetail, assertIs<ApiResult.Success<*>>(result).data)
    }
}
