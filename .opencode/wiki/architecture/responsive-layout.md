---
tags:
  - wiki/architecture
---

# Responsive Layout + Dual Pane

> **Last verified:** 2026-05-19 | **Verified by:** [source] — added ConnectivityObserver + OfflineBanner

## Estrategia adaptativa

El app usa un enfoque de **single codebase, two layouts** con `BoxWithConstraints`:

```
ResponsiveApp
    │
    ├── maxWidth < 840dp  →  MainNavigation (phone, single pane)
    │
    └── maxWidth >= 840dp →  DualPaneScreen (tablet, split pane)
```

El breakpoint de **840dp** es el umbral estándar de Material 3 para tablet.

## ResponsiveApp

`ResponsiveApp` recibe un `Flow<ConnectivityStatus>` desde `MainActivity` y superpone un `OfflineBanner` animado cuando no hay conexión:

```kotlin
@Composable
fun ResponsiveApp(
    connectivityStatus: Flow<ConnectivityStatus>,
    modifier: Modifier = Modifier,
) {
    var selectedArticleId by rememberSaveable { mutableStateOf<Int?>(null) }
    val status by connectivityStatus.collectAsState(initial = ConnectivityStatus.Available)

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth < DUAL_PANE_BREAKPOINT) {
                MainNavigation(
                    selectedArticleId = selectedArticleId,
                    onArticleSelected = { selectedArticleId = it },
                )
            } else {
                DualPaneScreen(
                    selectedArticleId = selectedArticleId,
                    onArticleSelected = { selectedArticleId = it },
                )
            }
        }

        OfflineBanner(
            connectivityStatus = status,
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
        )
    }
}
```

- `selectedArticleId` se comparte entre ambos layouts.
- `rememberSaveable` sobrevive a cambios de configuración (rotación, fold/unfold).
- En phone, el estado se usa para navegación programática desde `LaunchedEffect` en `MainNavigation`.
- `ConnectivityObserver` (inyectado en `MainActivity`) expone `Flow<ConnectivityStatus>` reactivo vía `ConnectivityManager.NetworkCallback`.
- `OfflineBanner` usa `AnimatedVisibility` con slide vertical, aparece sobre la UI principal sin interrumpir la navegación. [source]

## MainNavigation (phone)

```kotlin
@Composable
fun MainNavigation(selectedArticleId: Int?, onArticleSelected: (Int) -> Unit) {
    val navController = rememberNavController()

    LaunchedEffect(selectedArticleId) {
        selectedArticleId?.let { id ->
            navController.popBackStack(ArticlesRoute, inclusive = false)
            navController.navigate(DetailRoute(id))
        }
    }

    NavHost(navController, startDestination = ArticlesRoute) {
        composable<ArticlesRoute> { /* list screen */ }
        composable<DetailRoute> { /* detail screen */ }
    }
}
```

- Navegación normal: list → detail via `NavController.navigate()`.
- Slide horizontal: 300ms `tween` con `slideInHorizontally/slideOutHorizontally`.
- El `LaunchedEffect` permite a `ResponsiveApp` forzar navegación al detalle; hace `popBackStack` antes de `navigate` para evitar ciclos en el back stack.

## DualPaneScreen (tablet)

```kotlin
@Composable
fun DualPaneScreen(
    selectedArticleId: Int?,
    onArticleSelected: (Int) -> Unit,
    listViewModel: ArticlesListViewModel = hiltViewModel(),
    detailViewModel: ArticleDetailPaneViewModel = hiltViewModel(),
) {
    Row {
        Box(Modifier.weight(LIST_WEIGHT)) {
            ArticlesListScreen(listViewModel, onArticleSelected)
        }
        VerticalDivider()
        Box(Modifier.weight(DETAIL_WEIGHT)) {
            DetailPane(selectedArticleId, detailViewModel)
        }
    }
}
```

- **Split: 40/60** — lista ocupa 40% del ancho, detalle 60%.
- **VerticalDivider** entre paneles como separador visual.
- Ambos ViewModels son `hiltViewModel()` independientes, cada uno con su propio scope.
- El detail pane recibe `articleId` imperativamente via `viewModel.loadArticle(id)`, no via `SavedStateHandle`.

## ArticleDetailPaneViewModel

```kotlin
@HiltViewModel
class ArticleDetailPaneViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<ArticleDetailState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ArticleDetailState>> = _uiState.asStateFlow()

    fun loadArticle(articleId: Int) {
        analytics.logScreenView("ArticleDetail_$articleId")
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            getArticle(articleId)
                .onSuccess { article ->
                    _uiState.value = UiState.Success(ArticleDetailState(article = article))
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
```

A diferencia de `ArticleDetailViewModel` (phone), este ViewModel:
- **No usa `SavedStateHandle`**: el `articleId` se pasa por parámetro a `loadArticle()`.
- **Reutiliza `GetArticleUseCase`** con timeout de 30s.
- **Cada clic en un artículo distinto** dispara `loadArticle()` → `UiState.Loading` → fetch → Success/Error.

## Estados del DetailPane

| `articleId` | UI |
|---|---|
| `null` | Placeholder: "Select an article" centrado |
| `!= null` | `LaunchedEffect(articleId)` → loading → contenido/error |

## Por qué dos ViewModels de detalle

`ArticleDetailViewModel` usa `SavedStateHandle` para extraer el `articleId` de los argumentos de navegación, lo cual solo funciona con Navigation Compose. `ArticleDetailPaneViewModel` recibe el ID imperativamente porque en tablet no hay navegación — el detail pane es un componente fijo que reacciona a selecciones del list pane.

## Ver también

- [[architecture/app-structure]] — Estructura completa del proyecto
- [[patterns/mvvm-repository]] — MVVM + Repository
- [[architecture/di-hierarchy]] — Hilt DI modules
