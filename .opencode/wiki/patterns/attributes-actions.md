---
tags:
  - wiki/pattern
---

# Attributes / Actions Pattern

> **Last verified:** 2026-08-04 | **Verified by:** [source] — documentada la acción de delete + navegación state-driven

## Qué es

Patrón de UI donde los composables de pantalla reciben dos parámetros tipados:

- **`Attributes`** — data class inmutable con todo el estado/datos que la pantalla necesita
- **`Actions`** — data class con todas las funciones callback que la pantalla puede disparar

La pantalla es una función pura: `f(attributes, actions) → UI`

## Estructura de archivos

Cada screen tiene 3 archivos:

```
ScreenState.kt      → Attributes + Actions data classes
Screen.kt           → Composable puro (sin Hilt, sin ViewModel)
ScreenRoute.kt      → Connector: hiltViewModel() → mapea a attributes/actions
```

## Ejemplo

```kotlin
// State
data class ArticlesListAttributes(
    val searchQuery: String,
    val articles: Flow<PagingData<Article>>,   // reactivo (Paging 3)
)

data class ArticlesListActions(
    val onSearchTextChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onArticleClick: (Int) -> Unit,
    val sendAnalytics: (String, Map<String, String>) -> Unit,  // provisto por ViewModel
)

// Screen (pura, sin ViewModel)
@Composable
fun ArticlesListScreen(attributes: ArticlesListAttributes, actions: ArticlesListActions) {
    val articles = attributes.articles.collectAsLazyPagingItems()
    // usa actions.onSearchTextChange, actions.onArticleClick, etc.
}

// Route (conecta ViewModel → Screen)
@Composable
fun ArticlesListScreenRoute(onArticleClick: (Int) -> Unit) {
    val vm = hiltViewModel()
    ArticlesListScreen(
        attributes = ArticlesListAttributes(searchQuery = vm.searchQuery, articles = vm.articles),
        actions = ArticlesListActions(
            onSearchTextChange = vm::onSearchTextChange,
            onClearSearch = vm::clearSearch,
            onArticleClick = onArticleClick,
            sendAnalytics = vm::sendAnalytics,
        ),
    )
}
```

## Beneficios

| Beneficio | Descripción |
|-----------|-------------|
| **Previews sin Hilt** | Se pueden pasar datos fake y lambdas no-op |
| **Screen pura** | Sin dependencia de ViewModel, Hilt, Lifecycle |
| **Testeabilidad** | Se puede testear la UI sin viewModel real |
| **API explícita** | La firma de la screen documenta todo lo que necesita |
| **Consistente** | Mismo patrón en todas las screens del proyecto |

## Relación con el flujo de datos

```
ViewModel (Hilt)
  │
  ├── searchQuery: StateFlow<String>   │
  ├── articles: Flow<PagingData<Article>>
  ├── sendAnalytics(event, props)     ← wrapper de AnalyticsHelper
  └── onArticleSelected(id)           ← tablet state
       │
       ▼
   Route composable
       │
       ├── attributes
       └── actions
            │
            ▼
       Screen (pura)
            │
            ├── collectAsLazyPagingItems()
            ├── SearchBar → actions.onSearchTextChange
            ├── Card click → actions.sendAnalytics() + actions.onArticleClick()
            └── etc.
```

## Analytics en Actions

El `sendAnalytics` se pasa desde el ViewModel via Actions:

```kotlin
// ViewModel: wrapper público
fun sendAnalytics(event: String, properties: Map<String, String>) {
    analyticsHelper.sendEvent(event, properties)
}

// Screen: se llama antes del onClick
articleCardSettings(
    article = article,
    onClick = {
        actions.sendAnalytics("article_selected", mapOf("id" to article.id.toString()))
        actions.onArticleClick(article.id)
    },
)()
```

Esto mantiene:
- **Eventos de ciclo de vida** (`screen_view`) → en el `init` del ViewModel
- **Eventos de usuario** (clicks) → en la Screen via `sendAnalytics`

## Navegación disparada desde el estado

`ArticleDetailScreen` tiene una acción de delete en el `TopAppBar` que necesita navegar hacia atrás **después** de ejecutarse. Como MVVM no tiene canal de side effects, la navegación se modela con un flag dentro del estado [source]:

```kotlin
// ArticleDetailViewModel.kt — el estado lleva el flag
data class ArticleDetailState(
    val article: Article,
    val navigateBack: Boolean = false,
)

fun onDeleteArticle() {
    val currentState = _uiState.value
    if (currentState !is UiState.Success) return
    viewModelScope.launch {
        analytics.logEvent("article_deleted", mapOf("id" to articleId.toString()))
        _uiState.update { state ->
            if (state is UiState.Success) {
                state.copy(data = state.data.copy(navigateBack = true))
            } else state
        }
    }
}

// ArticleDetailScreenRoute.kt — el Route observa el flag y navega
LaunchedEffect(state) {
    if ((state as? UiState.Success)?.data?.navigateBack == true) {
        onBack()
    }
}
```

Las Actions del detalle quedan así:

```kotlin
data class ArticleDetailActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit = {},
    val onDelete: () -> Unit = {},
)
```

> **Nota:** `onDeleteArticle()` **no borra nada** — no toca Room ni la API. Solo loguea el evento `article_deleted` y marca `navigateBack = true`. Es una acción de UI, no una operación de datos. [source]

**Tradeoff del flag en el estado:** es el equivalente MVVM de un side effect. Funciona, pero el flag queda pegado en el estado después de consumirse (no hay reseteo), así que si la pantalla no se destruye tras navegar, el `LaunchedEffect` puede re-dispararse. La alternativa formal es un `Channel<SideEffect>` — ver [[patterns/mvvm-vs-mvi]] y [[patterns/mvi-migration]]. [analysis]

## Previews

Los previews viven en `ui/preview/` y cubren todos los estados de cada pantalla:

| Pantalla | Estados |
|----------|---------|
| ArticlesListScreen | loading, card light/dark |
| ArticleDetailScreen | loading, éxito, error |

> **Nota:** Solo se puede previewar el estado de loading de `ArticlesListScreen` porque `collectAsLazyPagingItems()` requiere `AndroidUiDispatcher.Main` (un Looper real) que no está disponible en Compose Preview. Los datos vía `PagingData.from()` no se colectan en previews.

Ejemplo de preview funcional (loading):

```kotlin
@Preview(showBackground = true)
@Composable
private fun ArticlesListScreenLoadingPreview() {
    MaterialTheme {
        ArticlesListScreen(
            attributes = ArticlesListAttributes(
                searchQuery = "",
                articles = flow { },
            ),
            actions = ArticlesListActions(
                onSearchTextChange = {},
                onClearSearch = {},
                onArticleClick = {},
                sendAnalytics = { _, _ -> },
            ),
        )
    }
}
```

## Archivos del proyecto

| Archivo | Propósito |
|---------|-----------|
| `ui/articles/list/ArticlesListScreenState.kt` | Attributes + Actions |
| `ui/articles/list/ArticlesListScreen.kt` | Screen pura |
| `ui/articles/list/ArticlesListScreenRoute.kt` | Connector con ViewModel |
| `ui/articles/detail/ArticleDetailScreenState.kt` | Attributes + Actions |
| `ui/articles/detail/ArticleDetailScreen.kt` | Screen pura |
| `ui/articles/detail/ArticleDetailScreenRoute.kt` | Connector con ViewModel |
| `ui/preview/ArticlesListScreenPreviews.kt` | 3 previews (loading + cards) |
| `ui/preview/ArticleDetailScreenPreviews.kt` | 3 previews (detail) |

## Ver también

- [[patterns/mvvm-repository]] — MVVM + Repository
- [[architecture/app-structure]] — Estructura general del app
