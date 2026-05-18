# MVVM + Repository Pattern

> **Last verified:** 2026-05-17 | **Verified by:** [source]

## Estructura por feature

Cada feature tiene 4 archivos en `ui/articles/<feature>/`:

| Archivo | Responsabilidad |
|---|---|
| `*Screen.kt` | Composable con la UI. Recibe state + callbacks. |
| `*ScreenRoute.kt` | Composable route que instancia el ViewModel via `hiltViewModel()` y mapea state a screen |
| `*ScreenState.kt` | Data classes con el estado de la UI |
| `*ViewModel.kt` | ViewModel que expone `StateFlow<UiState<T>>` |

## Flujo de datos

```
View (Composable)
    ↑ StateFlow<UiState<T>>
ViewModel
    ↑ Result<T> / Flow<PagingData<T>>
Repository
    ↑ API Response / Room DAO
ApiService / ArticleDao
```

## UiState

```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

Cada ViewModel expone `StateFlow<UiState<T>>`. La Screen observa con `collectAsStateWithLifecycle()`.

## ViewModel

- Usa `ViewModelProvider` de Hilt via `hiltViewModel()` en el Route
- Inyecta Repository via constructor `@Inject constructor`
- Usa `viewModelScope.launch` para corrutinas
- Expone estado via `StateFlow`, acciones via métodos públicos

## Repository

- Inyecta ApiService + DAOs via constructor
- Devuelve `Result<T>` para operaciones puntuales (getArticleById)
- Devuelve `Flow<PagingData<T>>` para listas paginadas (getArticles, searchArticles)
- Wrappea errores de red en `ApiException`