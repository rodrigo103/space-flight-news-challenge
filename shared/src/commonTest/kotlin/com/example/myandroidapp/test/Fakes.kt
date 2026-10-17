package com.example.myandroidapp.test

import androidx.paging.PagingSource
import com.example.myandroidapp.data.local.ArticleDao
import com.example.myandroidapp.data.local.ArticleEntity
import com.example.myandroidapp.data.remote.ApiResult
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.data.remote.configureApiClient
import com.example.myandroidapp.domain.model.Article
import com.example.myandroidapp.domain.repository.ArticlesRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import androidx.paging.PagingData

/**
 * FAKE — MockK no corre en Kotlin/Native, así que los dobles de prueba son fakes escritos a mano
 * (implementación simplificada pero funcional). Este es funcional de verdad: un `MutableMap` como
 * storage real; `inserted` expone lo persistido y las queries de paging no se ejercitan.
 */
class FakeArticleDao(
    initial: List<ArticleEntity> = emptyList(),
) : ArticleDao {
    private val storage = initial.associateBy { it.id }.toMutableMap()

    val inserted: List<ArticleEntity> get() = storage.values.toList()

    override suspend fun insertAll(articles: List<ArticleEntity>) {
        articles.forEach { storage[it.id] = it }
    }

    override suspend fun insert(article: ArticleEntity) {
        storage[article.id] = article
    }

    override suspend fun getById(id: Int): ArticleEntity? = storage[id]

    override suspend fun clearAll() {
        storage.clear()
    }

    // Paging no se ejercita en estos tests; el repositorio se testea por sus métodos puntuales.
    override fun pagingSource(): PagingSource<Int, ArticleEntity> =
        throw NotImplementedError("Paging no se testea acá")

    override fun searchPagingSource(query: String): PagingSource<Int, ArticleEntity> =
        throw NotImplementedError("Paging no se testea acá")
}

/**
 * FAKE — versión simplificada pero funcional del repositorio: campos configurables
 * (`articleResult`, `cachedArticle`, `getArticleDelayMs`) que cada test setea antes de invocar, y
 * `getArticleCallCount` para quien quiera verificar llamadas (no se usa en los tests actuales).
 */
class FakeArticlesRepository : ArticlesRepository {
    var articleResult: ApiResult<Article> = ApiResult.Failure(IllegalStateException("not set"))
    var cachedArticle: Article? = null
    var getArticleDelayMs: Long = 0
    var getArticleCallCount = 0
        private set

    override suspend fun getArticles(limit: Int, offset: Int): ApiResult<List<Article>> =
        ApiResult.Success(emptyList())

    override suspend fun searchArticles(query: String, limit: Int): ApiResult<List<Article>> =
        ApiResult.Success(emptyList())

    override suspend fun getArticle(id: Int): ApiResult<Article> {
        getArticleCallCount++
        if (getArticleDelayMs > 0) kotlinx.coroutines.delay(getArticleDelayMs)
        return articleResult
    }

    override suspend fun getCachedArticle(id: Int): Article? = cachedArticle

    override fun getArticlesPaged(searchQuery: String?): Flow<PagingData<Article>> =
        flowOf(PagingData.empty())
}

/**
 * SPY — registra las interacciones (las URLs de las requests que pasan por el engine) para poder
 * assertearlas después: query params, params de búsqueda, etc.
 */
class RecordedRequests {
    val urls = mutableListOf<String>()
}

/**
 * Helper que arma un STUB — cliente con la MISMA configuración que producción
 * (`configureApiClient`), montado sobre `MockEngine`: responde status/body enlatados a toda
 * request. Así el mapeo de status codes a `ApiException` que se testea es el real. Si recibe un
 * [RecordedRequests] (SPY), además registra las URLs.
 */
fun testApiService(
    status: HttpStatusCode = HttpStatusCode.OK,
    body: String = "{}",
    recorded: RecordedRequests? = null,
): ApiService {
    val engine = MockEngine { request ->
        recorded?.urls?.add(request.url.toString())
        respond(
            content = ByteReadChannel(body),
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
    return ApiService(HttpClient(engine) { configureApiClient() })
}
