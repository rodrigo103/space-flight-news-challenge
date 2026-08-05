---
tags:
  - wiki/pattern
---

# Error Handling

> **Last verified:** 2026-08-05 | **Verified by:** [source] — migrado de `CallAdapter.Factory` a `HttpResponseValidator` de Ktor, y de `Result` a `ApiResult`

> **Cambios de las fases 2+3 de [[patterns/kmp-migration]]:** el `HttpErrorCallAdapterFactory` fue
> reemplazado por `HttpResponseValidator` de Ktor **con el mismo mapeo de status codes**, y el
> repositorio devuelve `ApiResult<T>` en vez de `Result<T>` (`Result` no se exporta a Swift).
> La jerarquía `ApiException` no cambió. Detalle en [[architecture/data-layer]].

## Capas de error

### Network layer

- `HttpErrorCallAdapterFactory` — `CallAdapter.Factory` registrado en el `Retrofit.Builder` de `NetworkModule`. Intercepta `Call.enqueue()` y convierte respuestas no-2xx en excepciones tipadas **antes** de que lleguen al caller. [source]
- `ApiException` — sealed class con subclases por HTTP status [source]:
  - `BadRequest(400)`, `Unauthorized(401)`, `NotFound(404)`, `Conflict(409)`
  - `ServerError(code, 500..599)`, `NetworkError`
- **Ya no se usa `Response<T>` en las APIs** ni `extractBody()`. Las APIs devuelven dominio directo (`Article`, `ArticleResponse`). El `CallAdapter` se encarga de mapear errores HTTP a excepciones automáticamente.

### Repository layer

- Repository wrappea operaciones con `Result.runCatching { ... }`
- Errores de red → `Result.failure(ApiException)` (lanzada por el CallAdapter)
- Errores de base de datos → `Result.failure(Exception)` (Room lanza sus propias excepciones)
- Logging via `Timber.e()` en `.onFailure {}`

### ViewModel layer

- ViewModel transforma `Result<T>` en `UiState<T>`:
- `Result.success` → `UiState.Success`
- `Result.failure` → `UiState.Error(message)`

### UI layer

- `Success` → renderiza datos
- `Loading` → muestra indicador de carga (Lottie)
- `Error` → muestra Snackbar con mensaje + botón de retry

## Patrón general

```kotlin
// En Repository
override suspend fun getArticle(id: Int): Result<Article> =
    runCatching {
        apiService.getArticle(id)  // CallAdapter tira ApiException en error
    }.onFailure {
        Timber.e(it, "Error fetching article with id: %d", id)
    }

// En ViewModel
fun loadArticle(id: Int) {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        repository.getArticle(id)
            .onSuccess { _uiState.value = UiState.Success(it) }
            .onFailure { _uiState.value = UiState.Error(it.message ?: "Error desconocido") }
    }
}
```

## Errores no controlados

- `MyApplication` tiene `Thread.setDefaultUncaughtExceptionHandler`
- LeakCanary en debug captura memory leaks

## Timeout pattern

Para operaciones que pueden colgarse, usar `withTimeoutOrNull`:

```kotlin
val success = withTimeoutOrNull(30_000L) {
    repository.fetchArticles()
}
if (success == null) {
    _uiState.value = UiState.Error("Tiempo de espera agotado. Verificá tu conexión.")
}
```

El botón de retry reinicia el flujo completo.

## Retry pattern

```kotlin
fun retry() {
    loadArticles()  // reinicia el flow desde el principio
}
```

## Error handling jerárquico

La jerarquía de excepciones de red permite manejo granular:

```kotlin
sealed class ApiException(code: Int, message: String) : Exception(message) {
    class BadRequest(message: String) : ApiException(400, message)
    class Unauthorized(message: String) : ApiException(401, message)
    class NotFound(message: String) : ApiException(404, message)
    class Conflict(message: String) : ApiException(409, message)
    class ServerError(code: Int, message: String) : ApiException(code, message)
    class NetworkError(cause: Throwable) : ApiException(0, cause.message ?: "Network error")
}
```

Esto permite al ViewModel reaccionar distinto según el tipo de error:

```kotlin
repository.getArticle(id)
    .onSuccess { _uiState.value = UiState.Success(it) }
    .onFailure { when (it) {
        is ApiException.NotFound -> _uiState.value = UiState.Error("Artículo no encontrado")
        is ApiException.ServerError -> _uiState.value = UiState.Error("Error del servidor. Intenta más tarde.")
        else -> _uiState.value = UiState.Error("Error de conexión")
    }}
```

## Ver también

- [[tools/retrofit-setup]] — HTTP error handling via CallAdapter
- [[tools/testing-strategy]] — Testing de errores con MockWebServer
