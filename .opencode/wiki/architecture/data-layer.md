---
tags:
  - wiki/architecture
---

# Data Layer

> **Last verified:** 2026-08-05 | **Verified by:** [source] — migrada a `:shared` con Ktor + Room KMP (fases 2+3 de [[patterns/kmp-migration]])

Data layer del proyecto. Vive **entero en `:shared/commonMain`**, así que compila para Android e iOS. Sigue patrón Repository con dos fuentes: API remota (Ktor) y base local (Room KMP).

## Network layer (`data/remote/`)

```
ApiService (Ktor)
    ├── GET /articles/?limit=&offset=&search=  → ArticleResponse
    └── GET /articles/{id}/                     → Article
```

- Base URL: `https://api.spaceflightnewsapi.net/v4` — API agregadora, no hostea contenido completo
- Ambos endpoints devuelven el **mismo schema**. No existe campo `body`/`content`. [source]
- `ApiService` es una **clase** que recibe un `HttpClient`, no una interfaz anotada. Retrofit no es multiplataforma. [source]
- `kotlinx.serialization` vía `ContentNegotiation` con `ignoreUnknownKeys = true`
- Engine por plataforma con `expect/actual`: **OkHttp** en `androidMain`, **Darwin** en `iosMain`

### Error handling: `HttpResponseValidator`

Reemplaza al `HttpErrorCallAdapterFactory` de Retrofit, con el mismo mapeo [source]:

| HTTP status | Exception |
|---|---|
| 400 | `ApiException.BadRequest` |
| 401 | `ApiException.Unauthorized` |
| 404 | `ApiException.NotFound` |
| 409 | `ApiException.Conflict` |
| 500..599 | `ApiException.ServerError(code, message)` |
| Otro ≥300 | `ApiException.NetworkError` |

La config vive en `configureApiClient()`, compartida por producción y tests — el mapeo que se testea es el que corre. [analysis]

### `ApiResult` en vez de `Result`

La API pública del módulo devuelve `ApiResult<T>`, una sealed interface, **no `kotlin.Result`**:

```kotlin
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: Throwable) : ApiResult<Nothing>
}
```

`Result` es una value class y **no se exporta al framework de Objective-C**: el tipo se borra a `id` (`Any?` en Swift), perdiendo incluso el tipo del dato exitoso, y compila sin warnings. Ver la evidencia en [[patterns/kmp-migration]]. [source]

## Room layer (`data/local/`)

Room KMP 2.8.4 + `sqlite-bundled`. **Los DAOs, entidades y mappers se movieron sin cambios de código.** [source]

```kotlin
@Database(entities = [ArticleEntity::class], version = 1, exportSchema = true)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
```

- `BundledSQLiteDriver` — SQLite embebido, mismo binario en las dos plataformas
- Builder por plataforma: `Context.getDatabasePath()` en Android, `NSFileManager` + `NSDocumentDirectory` en iOS
- KSP corre por target: `kspAndroid`, `kspIosX64`, `kspIosArm64`, `kspIosSimulatorArm64`
- `ArticleDao` con `pagingSource()`, `searchPagingSource()`, `insertAll()`, `getById()`, `clearAll()`

## Paging 3 + RemoteMediator

`ArticleRemoteMediator` sincroniza API → Room. `paging-common` y `room-paging` publican variantes iOS, así que vive en `commonMain`.

Las excepciones de red cambiaron: antes `java.net.UnknownHostException` / `ConnectException` / `SocketTimeoutException`, ahora **`kotlinx.io.IOException`**, que es lo que tira Ktor. El comportamiento se mantiene: fallo de red → `Success(endOfPaginationReached = false)` para no perder la caché; error HTTP (que llega como `ApiException`, no `IOException`) → `MediatorResult.Error`. [source]

> **Nota:** `Flow<PagingData>` no es consumible desde SwiftUI. iOS usará `getArticles(limit, offset)`. Ver [[patterns/kmp-migration]].

## Repository pattern

Interfaz en `domain/repository/`, implementación en `data/repository/`, ambas en `commonMain`.

`DefaultArticlesRepository` [source]:
- `getArticles(limit, offset)` → `ApiResult<List<Article>>`
- `searchArticles(query, limit)` → `ApiResult<List<Article>>`
- `getArticle(id)` → `ApiResult<Article>`
- `getCachedArticle(id)` → `Article?` (Room)
- `getArticlesPaged(searchQuery?)` → `Flow<PagingData<Article>>`
- Errores capturados con `apiRunCatching { }`, que relanza `CancellationException`
- Logging vía `AppLogger` (`expect/actual`), porque Timber es JVM-only

## Qué NO conoce `:app`

`:shared` expone factories que devuelven sus propios tipos (`createApiService()`, `createDatabase(context)`), así que **Ktor y Room no aparecen en ninguna firma de `:app`**. Los módulos Hilt construyen contra esas factories. [source]

## Ver también

- [[patterns/kmp-migration]] — Plan y estado de la migración
- [[architecture/app-structure]] — Estructura de módulos
- [[patterns/room-paging]] — Room + Paging 3 + RemoteMediator
- [[patterns/error-handling]] — Manejo de errores
