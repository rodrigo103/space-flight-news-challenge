---
tags:
  - wiki/pattern
  - architecture
---

# MVI Migration

> **Last verified:** 2026-05-29 | **Verified by:** [analysis]

## Contexto

Este documento describe cómo migrar la arquitectura actual (MVVM con Attributes/Actions) a MVI (Model-View-Intent) completo, y también propone una versión "MVI Lite" más pragmática para un proyecto de este tamaño.

La arquitectura actual ya tiene **flujo de datos unidireccional** (StateFlow hacia abajo, callbacks hacia arriba). Lo que MVI agrega es formalizar los eventos, unificar el estado, y separar los efectos secundarios.

---

## Estado actual vs MVI

| Concepto MVI | Qué ya tenés | Qué falta |
|---|---|---|
| **Model** (State) | `StateFlow<UiState<T>>` + `Flow<PagingData>` | Estado disperso en varios `StateFlow`; el list ViewModel no tiene un state unificado |
| **View** (UI) | `Attributes` (state down) + `Actions` (callbacks up) | Ya es UDF puro — la View no conoce al ViewModel |
| **Intent** (Event) | Lambdas sueltas en `Actions` (ej. `onSearchTextChange: (String) -> Unit`) | Sin estructura formal — cada acción es una lambda independiente |
| **Side Effects** | No modelados — Snackbar y navegación se manejan en el composable directamente | No hay canal de efectos secundarios; no hay forma de testearlos sin la UI |

---

## MVI completo — Migración archivo por archivo

### 1. `ArticlesListScreenState.kt`

**Antes (MVVM — Attributes + Actions):**

```kotlin
data class ArticlesListAttributes(
    val searchQuery: String,
    val articles: Flow<PagingData<Article>>,
)

data class ArticlesListActions(
    val onSearchTextChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onArticleClick: (Int) -> Unit,
    val sendAnalytics: (String, Map<String, String>) -> Unit,
)
```

**Después (MVI — State + Event + SideEffect):**

```kotlin
data class ArticlesListState(
    val searchQuery: String = "",
    val articles: Flow<PagingData<Article>>,
)

sealed interface ArticlesListEvent {
    data class SearchQueryChanged(val query: String) : ArticlesListEvent
    data object ClearSearch : ArticlesListEvent
    data class ArticleClicked(val articleId: Int) : ArticlesListEvent
}

sealed interface ArticlesListSideEffect {
    data class NavigateToDetail(val articleId: Int) : ArticlesListSideEffect
}
```

### 2. `ArticlesListViewModel.kt`

**Antes (MVVM):**

```kotlin
@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        analytics.logScreenView("ArticlesList")
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val articles: Flow<PagingData<Article>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            repository.getArticlesPaged(searchQuery = query.ifBlank { null })
        }
        .cachedIn(viewModelScope)

    fun onSearchTextChange(text: String) {
        _searchQuery.value = text
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun sendAnalytics(event: String, properties: Map<String, String>) {
        analytics.logEvent(event, properties)
    }
}
```

**Después (MVI):**

```kotlin
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(ArticlesListState(
        searchQuery = "",
        articles = emptyFlow(),
    ))

    val state: StateFlow<ArticlesListState> = combine(
        _state,
        _state.map { it.searchQuery }
            .debounce(300)
            .flatMapLatest { query ->
                repository.getArticlesPaged(searchQuery = query.ifBlank { null })
            }
    ) { currentState, articles ->
        currentState.copy(articles = articles)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArticlesListState())

    private val _sideEffects = Channel<ArticlesListSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ArticlesListSideEffect> = _sideEffects.receiveAsFlow()

    init {
        analytics.logScreenView("ArticlesList")
    }

    fun onEvent(event: ArticlesListEvent) {
        when (event) {
            is ArticlesListEvent.SearchQueryChanged ->
                _state.update { it.copy(searchQuery = event.query) }

            is ArticlesListEvent.ClearSearch ->
                _state.update { it.copy(searchQuery = "") }

            is ArticlesListEvent.ArticleClicked -> {
                analytics.logEvent("article_selected", mapOf("id" to event.articleId.toString()))
                viewModelScope.launch {
                    _sideEffects.send(ArticlesListSideEffect.NavigateToDetail(event.articleId))
                }
            }
        }
    }
}
```

**Explicación del `combine()`:** Paging 3 usa `Flow<PagingData>` que es reactivo por sí mismo — no se "reduce" como un valor puntual. La solución es derivar el Flow de artículos a partir del `searchQuery` en el state, y combinarlos de vuelta en un único `StateFlow<ArticlesListState>` con `combine()`. El `stateIn()` asegura que el Flow sea consistente con el ciclo de vida del ViewModel.

### 3. `ArticlesListScreen.kt`

**Antes — recibe Attributes y Actions separados:**

```kotlin
@Composable
fun ArticlesListScreen(
    attributes: ArticlesListAttributes,
    actions: ArticlesListActions,
    modifier: Modifier = Modifier,
) {
    val articles = attributes.articles.collectAsLazyPagingItems()
    // ...
    OutlinedTextField(
        value = attributes.searchQuery,
        onValueChange = actions.onSearchTextChange,
        trailingIcon = {
            if (attributes.searchQuery.isNotEmpty()) {
                IconButton(onClick = actions.onClearSearch) { /* ... */ }
            }
        }
    )
    // article click -> actions.onArticleClick(article.id)
}
```

**Después — recibe state unificado + un solo event handler:**

```kotlin
@Composable
fun ArticlesListScreen(
    state: ArticlesListState,
    onEvent: (ArticlesListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val articles = state.articles.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Space Flight News") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = modifier.padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onEvent(ArticlesListEvent.SearchQueryChanged(it)) },
                placeholder = { Text("Search articles...") },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onEvent(ArticlesListEvent.ClearSearch) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
            )

            when (val refreshState = articles.loadState.refresh) {
                is LoadState.Loading -> {
                    if (articles.itemCount == 0) ShimmerPage()
                    else ArticleList(articles, onEvent)
                }
                is LoadState.Error -> {
                    if (articles.itemCount == 0) ErrorMessage(onRetry = { articles.retry() })
                    else ArticleList(articles, onEvent)
                }
                is LoadState.NotLoading -> {
                    if (articles.itemCount == 0) EmptyState()
                    else ArticleList(articles, onEvent)
                }
            }
        }
    }
}

@Composable
private fun ArticleList(
    articles: LazyPagingItems<Article>,
    onEvent: (ArticlesListEvent) -> Unit,
) {
    LazyColumn {
        items(articles) { article ->
            article?.let {
                ArticleCard(
                    article = it,
                    onClick = { onEvent(ArticlesListEvent.ArticleClicked(it.id)) }
                )
            }
        }
    }
}
```

El contrato de la Screen se simplifica: de `(Attributes, Actions)` a `(State, onEvent)`. El compilador fuerza a manejar todos los casos de `ArticlesListEvent` en el ViewModel — no podés olvidarte de wirear una acción.

### 4. `ArticlesListScreenRoute.kt`

**Antes:**

```kotlin
@Composable
fun ArticlesListScreenRoute(
    onArticleClick: (Int) -> Unit,
    modifier: Modifier,
    viewModel: ArticlesListViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    ArticlesListScreen(
        attributes = ArticlesListAttributes(searchQuery, viewModel.articles),
        actions = ArticlesListActions(
            onSearchTextChange = viewModel::onSearchTextChange,
            onClearSearch = viewModel::clearSearch,
            onArticleClick = onArticleClick,
            sendAnalytics = viewModel::sendAnalytics,
        ),
        modifier = modifier,
    )
}
```

**Después:**

```kotlin
@Composable
fun ArticlesListScreenRoute(
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticlesListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is ArticlesListSideEffect.NavigateToDetail ->
                    onNavigateToDetail(effect.articleId)
            }
        }
    }

    ArticlesListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
```

**Explicación del `LaunchedEffect` para side effects:** Los side effects son eventos únicos (navegación, mostrar un toast) que no deben re-ejecutarse en recomposición. El `LaunchedEffect(Unit)` se lanza una sola vez y recolecta el `SharedFlow` del ViewModel. Cada efecto se consume una vez y se despacha al callback de navegación.

### 5. `ArticleDetailScreenState.kt`

**Antes:**

```kotlin
data class ArticleDetailAttributes(
    val state: UiState<ArticleDetailState>,
)

data class ArticleDetailActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit = {},
)
```

**Después:**

```kotlin
sealed interface ArticleDetailEvent {
    data object Retry : ArticleDetailEvent
    data object Back : ArticleDetailEvent
}

sealed interface ArticleDetailSideEffect {
    data object NavigateBack : ArticleDetailSideEffect
}
```

### 6. `ArticleDetailViewModel.kt`

**Antes:**

```kotlin
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val articleId: Int = checkNotNull(savedStateHandle["articleId"]) { "articleId required" }

    private val _uiState = MutableStateFlow<UiState<ArticleDetailState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ArticleDetailState>> = _uiState.asStateFlow()

    init {
        analytics.logScreenView("ArticleDetail_$articleId")
        loadArticle()
    }

    fun loadArticle() {
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            getArticle(articleId)
                .onSuccess { article ->
                    analytics.logEvent("article_loaded", mapOf("id" to article.id.toString()))
                    _uiState.value = UiState.Success(ArticleDetailState(article = article))
                }
                .onFailure { e ->
                    analytics.logError(e, "loadArticle_$articleId")
                    _uiState.value = UiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
```

**Después:**

```kotlin
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val articleId: Int = checkNotNull(savedStateHandle["articleId"]) { "articleId required" }

    private val _state = MutableStateFlow<UiState<ArticleDetailState>>(UiState.Loading)
    val state: StateFlow<UiState<ArticleDetailState>> = _state.asStateFlow()

    private val _sideEffects = Channel<ArticleDetailSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ArticleDetailSideEffect> = _sideEffects.receiveAsFlow()

    init {
        analytics.logScreenView("ArticleDetail_$articleId")
        loadArticle()
    }

    fun onEvent(event: ArticleDetailEvent) {
        when (event) {
            ArticleDetailEvent.Retry -> loadArticle()
            ArticleDetailEvent.Back -> {
                viewModelScope.launch {
                    _sideEffects.send(ArticleDetailSideEffect.NavigateBack)
                }
            }
        }
    }

    private fun loadArticle() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            getArticle(articleId)
                .onSuccess { article ->
                    analytics.logEvent("article_loaded", mapOf("id" to article.id.toString()))
                    _state.value = UiState.Success(ArticleDetailState(article = article))
                }
                .onFailure { e ->
                    analytics.logError(e, "loadArticle_$articleId")
                    _state.value = UiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
```

### 7. `ArticleDetailScreen.kt`

**Antes:**

```kotlin
@Composable
fun ArticleDetailScreen(
    attributes: ArticleDetailAttributes,
    actions: ArticleDetailActions,
    modifier: Modifier = Modifier,
)
```

**Después:**

```kotlin
@Composable
fun ArticleDetailScreen(
    state: UiState<ArticleDetailState>,
    onEvent: (ArticleDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Article") },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ArticleDetailEvent.Back) }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            is UiState.Loading -> LoadingContent(modifier.padding(padding))
            is UiState.Error -> ErrorContent(
                message = current.message,
                onRetry = { onEvent(ArticleDetailEvent.Retry) },
                modifier = modifier.padding(padding)
            )
            is UiState.Success -> ArticleDetailContent(
                article = current.data.article,
                modifier = modifier.padding(padding)
            )
        }
    }
}
```

### 8. `ArticleDetailScreenRoute.kt`

**Antes:**

```kotlin
@Composable
fun ArticleDetailScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ArticleDetailScreen(
        attributes = ArticleDetailAttributes(state = state),
        actions = ArticleDetailActions(onBack = onBack, onRetry = viewModel::loadArticle),
        modifier = modifier,
    )
}
```

**Después:**

```kotlin
@Composable
fun ArticleDetailScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                ArticleDetailSideEffect.NavigateBack -> onBack()
            }
        }
    }

    ArticleDetailScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
```

### 9. `ArticleDetailPaneViewModel.kt` — Tablets

El `ArticleDetailPaneViewModel` sigue el mismo patrón que `ArticleDetailViewModel`, con la diferencia de que recibe `articleId` como parámetro en `loadArticle(id)` en lugar de `SavedStateHandle`:

```kotlin
@HiltViewModel
class ArticleDetailPaneViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ArticleDetailState>>(UiState.Loading)
    val state: StateFlow<UiState<ArticleDetailState>> = _state.asStateFlow()

    private val _sideEffects = Channel<ArticleDetailSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ArticleDetailSideEffect> = _sideEffects.receiveAsFlow()

    private var currentArticleId: Int? = null

    fun onEvent(event: ArticleDetailEvent, articleId: Int? = currentArticleId) {
        when (event) {
            ArticleDetailEvent.Retry -> articleId?.let { loadArticle(it) }
            ArticleDetailEvent.Back -> {
                viewModelScope.launch {
                    _sideEffects.send(ArticleDetailSideEffect.NavigateBack)
                }
            }
        }
    }

    fun loadArticle(articleId: Int) {
        currentArticleId = articleId
        analytics.logScreenView("ArticleDetail_$articleId")
        viewModelScope.launch {
            _state.value = UiState.Loading
            getArticle(articleId)
                .onSuccess { article ->
                    analytics.logEvent("article_loaded", mapOf("id" to article.id.toString()))
                    _state.value = UiState.Success(ArticleDetailState(article = article))
                }
                .onFailure { e ->
                    analytics.logError(e, "loadArticle_$articleId")
                    _state.value = UiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
```

### 10. `DualPaneScreen.kt` — Tablets

El `DualPaneScreen` se adapta al nuevo contrato `(state, onEvent)`:

```kotlin
@Composable
fun DualPaneScreen(
    selectedArticleId: Int?,
    onArticleSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    listViewModel: ArticlesListViewModel = hiltViewModel(),
    detailViewModel: ArticleDetailPaneViewModel = hiltViewModel(),
) {
    val listState by listViewModel.state.collectAsStateWithLifecycle()

    Row(modifier = modifier) {
        Box(modifier = Modifier.weight(0.4f)) {
            ArticlesListScreen(
                state = listState,
                onEvent = { event ->
                    when (event) {
                        is ArticlesListEvent.ArticleClicked -> onArticleSelected(event.articleId)
                        else -> listViewModel.onEvent(event)
                    }
                },
            )
        }

        VerticalDivider()

        Box(modifier = Modifier.weight(0.6f)) {
            if (selectedArticleId == null) {
                Text("Select an article", modifier = Modifier.align(Alignment.Center))
            } else {
                LaunchedEffect(selectedArticleId) {
                    detailViewModel.loadArticle(selectedArticleId)
                }
                val detailState by detailViewModel.state.collectAsStateWithLifecycle()
                ArticleDetailContent(
                    state = detailState,
                    onEvent = { detailViewModel.onEvent(it, selectedArticleId) },
                )
            }
        }
    }
}
```

**Nota sobre DualPaneScreen:** Los eventos de `ArticleClicked` se interceptan en el `DualPaneScreen` para actualizar el `selectedArticleId` (navegación local). El resto de eventos se delegan al ViewModel normalmente.

### 11. `ResponsiveApp.kt` — Sin cambios estructurales

El `ResponsiveApp` sigue igual — solo cambia cómo se pasan los callbacks hacia abajo (de `onArticleClick` a interceptar eventos):

```kotlin
@Composable
fun ResponsiveApp(connectivityStatus: Flow<ConnectivityStatus>, modifier: Modifier = Modifier) {
    var selectedArticleId by rememberSaveable { mutableStateOf<Int?>(null) }
    val status by connectivityStatus.collectAsState(initial = ConnectivityStatus.Available)

    Box(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth < 840.dp) {
                MainNavigation(
                    selectedArticleId = selectedArticleId,
                    onArticleSelected = { selectedArticleId = it }
                )
            } else {
                DualPaneScreen(
                    selectedArticleId = selectedArticleId,
                    onArticleSelected = { selectedArticleId = it }
                )
            }
        }
        OfflineBanner(connectivityStatus = status, modifier = Modifier.align(Alignment.TopCenter))
    }
}
```

---

## Archivos que NO cambian

| Capa | Archivos | Razón |
|------|----------|-------|
| Domain | `Article.kt`, `ArticlesRepository.kt`, `GetArticleUseCase.kt` | La capa de datos es agnóstica al patrón UI |
| Data | `ApiService.kt`, `ArticleDao.kt`, `ArticleRemoteMediator.kt`, `DefaultArticlesRepository.kt`, `ArticleEntity.kt`, `ArticleMappers.kt` | La fuente de datos no cambia |
| DI | `AppModule.kt`, `NetworkModule.kt`, `DatabaseModule.kt`, `RepositoryModule.kt` | Los bindings de Hilt son idénticos |
| UI Common | `UiState.kt` | La sealed class Loading/Success/Error se mantiene igual |
| Navigation | `Routes.kt`, `Navigation.kt` | Las rutas tipadas no cambian |
| Components | `OfflineBanner.kt`, `GradientBackground`, `ShimmerPage` | Componentes puros sin estado |
| Theme | Todo `ui/theme/` | Idéntico |

---

## Resumen visual del cambio

```
MVVM (actual)                          MVI (propuesto)
─────────────                          ───────────────
ViewModel                              ViewModel
  ├─ MutableStateFlow(query)             ├─ MutableStateFlow<State>  ← unificado
  ├─ Flow<PagingData>                    ├─ Channel<SideEffect>      ← nuevo
  ├─ fun onSearchTextChange()            └─ fun onEvent(Event)       ← único entry point
  ├─ fun clearSearch()
  └─ fun sendAnalytics()              Screen
                                        └─ (state: State, onEvent: (Event) -> Unit)
Screen
  └─ (attributes: Attributes,         Route
       actions: Actions)                 ├─ collectAsStateWithLifecycle()
                                         └─ LaunchedEffect { sideEffects.collect {} }
Route
  ├─ collectAsStateWithLifecycle()
  └─ wirea Attributes + Actions
```

La diferencia estructural clave: **el ViewModel pasa de N métodos públicos a un solo `onEvent(Event)`, y la navegación pasa de ser un callback directo a ser un side effect emitido por el ViewModel**.

---

## MVI Lite — Alternativa pragmática

Para un proyecto de 2 pantallas y 3 ViewModels, MVI completo puede ser **over-engineering**. Esta variante captura el 80% del beneficio con el 20% del esfuerzo:

### Qué incluye MVI Lite

1. **Side effects canalizados:** Agregar `Channel<SideEffect>` + `SharedFlow` en cada ViewModel para navegación y snackbars. El Route los consume con `LaunchedEffect`.

2. **El resto se queda igual:** Mantener `Attributes`/`Actions`, mantener `MutableStateFlow` separados donde tenga sentido (ej. search query).

### Archivos a tocar en MVI Lite

| Archivo | Cambio |
|---------|--------|
| `ArticlesListScreenState.kt` | Agregar `ArticlesListSideEffect` sealed interface |
| `ArticlesListViewModel.kt` | Agregar `Channel<ArticlesListSideEffect>` + `val sideEffects: Flow<>` |
| `ArticlesListScreenRoute.kt` | Agregar `LaunchedEffect` para recolectar side effects |
| `ArticleDetailScreenState.kt` | Agregar `ArticleDetailSideEffect` sealed interface |
| `ArticleDetailViewModel.kt` | Agregar `Channel<ArticleDetailSideEffect>` + `val sideEffects: Flow<>` |
| `ArticleDetailScreenRoute.kt` | Agregar `LaunchedEffect` para recolectar side effects |
| `ArticleDetailPaneViewModel.kt` | Ídem |
| `DualPaneScreen.kt` | Adaptar para side effects |

**No se tocan:** Screen composables, Attributes/Actions, contratos de ViewModel (solo se agrega, no se quita).

### Ejemplo — `ArticleDetailViewModel` en MVI Lite

```kotlin
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val getArticle: GetArticleUseCase,
    private val analytics: AnalyticsHelper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val articleId: Int = checkNotNull(savedStateHandle["articleId"])

    private val _uiState = MutableStateFlow<UiState<ArticleDetailState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ArticleDetailState>> = _uiState.asStateFlow()

    // ÚNICO CAMBIO: canal de side effects
    private val _sideEffects = Channel<ArticleDetailSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ArticleDetailSideEffect> = _sideEffects.receiveAsFlow()

    init {
        analytics.logScreenView("ArticleDetail_$articleId")
        loadArticle()
    }

    fun loadArticle() { /* idéntico */ }

    fun onBack() {
        viewModelScope.launch {
            _sideEffects.send(ArticleDetailSideEffect.NavigateBack)
        }
    }
}
```

El Route recolecta el side effect:

```kotlin
@Composable
fun ArticleDetailScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                ArticleDetailSideEffect.NavigateBack -> onBack()
            }
        }
    }

    ArticleDetailScreen(
        attributes = ArticleDetailAttributes(state = state),
        actions = ArticleDetailActions(
            onBack = viewModel::onBack,
            onRetry = viewModel::loadArticle,
        ),
        modifier = modifier,
    )
}
```

---

## Comparación: Las tres opciones

| | MVVM (actual) | MVI Lite | MVI completo |
|---|---|---|---|
| **Estado** | `MutableStateFlow` separados | Igual | `StateFlow<State>` unificado |
| **Eventos UI→VM** | Lambdas en `Actions` | Igual | `sealed interface Event` + `fun onEvent()` |
| **Side effects** | Manejo inline en composable | `Channel` + `SharedFlow` en ViewModel | `Channel` + `SharedFlow` en ViewModel |
| **Testeabilidad** | Requiere UI para verificar navegación | Side effects testeables sin UI | Estado + side effects 100% testeables |
| **Archivos cambiados** | 0 | 8 | 12 |
| **Complejidad nueva** | 0 | Baja | Media |
| **Beneficio** | — | Efectos separados de UI | Máxima formalidad |

---

## Beneficios de migrar a MVI (completo o Lite)

1. **Testeabilidad:** Con side effects en el ViewModel, podés verificar que al hacer `onEvent(ArticleClicked(42))` el ViewModel emite `NavigateToDetail(42)` — sin necesitar la UI.

2. **Type safety en eventos:** `sealed interface Event` fuerza al compilador a verificar que manejás todos los casos en el `when` del ViewModel. Si agregás un evento nuevo y no lo manejás, no compila.

3. **Estado restaurable:** Un solo `StateFlow<ScreenState>` se puede guardar/restaurar con `SavedStateHandle` como JSON (útil para process death). Con MVVM tendrías que serializar cada `MutableStateFlow` por separado.

4. **Traza de eventos:** Si necesitás logging o analytics de todas las acciones del usuario, un `onEvent(Event)` centralizado es un punto único donde interceptar todo.

5. **Predecibilidad:** El estado solo cambia como resultado de un `Event`. No hay setters públicos ni mutaciones laterales. Esto elimina bugs donde el estado se modifica desde dos lugares distintos sin coordinación.

---

## Riesgos y tradeoffs

1. **Paging 3 es incómodo en MVI:** `Flow<PagingData>` es inherentemente reactivo — no se puede "reducir" como un snapshot. La solución con `combine()` funciona pero agrega complejidad. Alternativa: mantener `articles` como `Flow<PagingData>` separado del `State` y pasarlo como parámetro aparte.

2. **Boilerplate en proyectos chicos:** Para 2 pantallas, las `sealed interface` de eventos suman ~30 líneas extra por feature. No se amortiza hasta que tenés 5+ pantallas con lógica compleja.

3. **Channel management:** `Channel.BUFFERED` es necesario para no perder eventos, pero si el ViewModel está muy cargado, los eventos se encolan. Para apps chicas no es problema, pero hay que saber que existe.

4. **Curva de aprendizaje:** Un dev nuevo que lee el código tiene que entender el flujo: `onEvent → reduce state → emit SideEffect → LaunchedEffect consume SideEffect`. Con MVVM actual, el flujo es `función pública → actualiza StateFlow → UI recompone`.

---

## Recomendación final

| Tamaño del proyecto | Recomendación |
|---------------------|---------------|
| 1-2 pantallas, lógica simple | **MVVM** (lo que ya tenés) — MVI es overhead innecesario |
| 3-5 pantallas, navegación + side effects | **MVI Lite** — solo agregá el canal de side effects |
| 5+ pantallas, lógica compleja, equipo grande | **MVI completo** — la formalidad paga el boilerplate |

Para este proyecto (2 pantallas, 3 ViewModels), **MVI Lite es el sweet spot**. El canal de side effects te da testeabilidad y separación de concerns sin el boilerplate de eventos tipados ni el `combine()` para Paging 3.

Si el proyecto creciera a 5+ features, migrar de MVI Lite a MVI completo es incremental: solo hay que agregar las `sealed interface Event` y unificar los `MutableStateFlow` en un solo `State`.

---

## Ver también

- [[patterns/mvvm-repository]] — Arquitectura MVVM actual
- [[patterns/search-strategy]] — Cómo funciona la búsqueda con Paging 3
- [[patterns/error-handling]] — Manejo de errores con Result + UiState
- [[patterns/attributes-actions]] — Attributes/Actions pattern actual
- [[architecture/clean-architecture-guide]] — Clean Architecture y use cases
