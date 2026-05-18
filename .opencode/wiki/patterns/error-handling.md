# Error Handling

> **Last verified:** 2026-05-17 | **Verified by:** [source]

## Capas de error

### Network layer

- `HttpErrorCallAdapter` intercepta respuestas HTTP no exitosas y las convierte en `ApiException` [source]
- `ApiException` tiene `code: Int`, `message: String` [source]
- `ResponseExt` provee `Response<T>.toResult(): Result<T>` que mapea éxito a `Result.success` y error a `Result.failure(ApiException)` [source]

### Repository layer

- Repository wrappea operaciones en try/catch y devuelve `Result<T>`
- Errores de red → `Result.failure(ApiException)`
- Errores de base de datos → `Result.failure(Exception)` (Room lanza sus propias excepciones)

### ViewModel layer

- ViewModel transforma `Result<T>` en `UiState<T>`:
- `Result.success` → `UiState.Success`
- `Result.failure` → `UiState.Error(message)`

### UI layer

- `Success` → renderiza datos
- `Loading` → muestra indicador de carga
- `Error` → muestra Snackbar con mensaje + botón de retry

## Patrón general

```kotlin
// En Repository
suspend fun getArticleById(id: Int): Result<Article> = runCatching {
    apiService.getArticle(id).toResult().getOrThrow()
}

// En ViewModel
fun loadArticle(id: Int) {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        repository.getArticleById(id)
            .onSuccess { _uiState.value = UiState.Success(it) }
            .onFailure { _uiState.value = UiState.Error(it.message ?: "Error desconocido") }
    }
}
```

## Errores no controlados

- `MyApplication` tiene `Thread.setDefaultUncaughtExceptionHandler`
- LeakCanary en debug captura memory leaks