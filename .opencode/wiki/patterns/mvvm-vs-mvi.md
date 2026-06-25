---
tags:
  - wiki/pattern
  - architecture
---

# MVVM vs MVI en Android

> **Last verified:** 2026-05-29 | **Verified by:** [analysis] — basado en la migración real del proyecto

## ¿De qué va esta página?

Explica las diferencias entre MVVM y MVI en Android con ejemplos concretos del proyecto, sin teoría abstracta. Si querés ver cómo se migra de uno a otro, leé [[patterns/mvi-migration]].

---

## MVVM (Model-View-ViewModel)

### Estructura

```
View (Composable) ←→ ViewModel ←→ Model (Repository + Data Sources)
```

El ViewModel **expone estado** hacia la View y **recibe comandos** desde la View. La comunicación es bidireccional pero estructurada:

- **ViewModel → View**: `StateFlow<T>` (observable, reactivo)
- **View → ViewModel**: Métodos públicos sueltos (`fun onSearchTextChange(text: String)`)

### Cómo se ve en código real

```kotlin
// ViewModel — MVVM
@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
) : ViewModel() {

    // Estado disperso: múltiples StateFlow independientes
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val articles: Flow<PagingData<Article>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query -> repository.getArticlesPaged(query) }
        .cachedIn(viewModelScope)

    // Comandos sueltos: cualquier método público es una "acción permitida"
    fun onSearchTextChange(text: String) { _searchQuery.value = text }
    fun clearSearch() { _searchQuery.value = "" }
    fun sendAnalytics(event: String, properties: Map<String, String>) { ... }
}

// View — MVVM: recibe Attributes (lectura) y Actions (escritura)
@Composable
fun ArticlesListScreen(
    attributes: ArticlesListAttributes,   // Estado para leer
    actions: ArticlesListActions,         // Callbacks para escribir
) {
    OutlinedTextField(
        value = attributes.searchQuery,
        onValueChange = actions.onSearchTextChange,  // ← lambda suelta
    )
}

// Actions: un bolso de lambdas
data class ArticlesListActions(
    val onSearchTextChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onArticleClick: (Int) -> Unit,
    val sendAnalytics: (String, Map<String, String>) -> Unit,
)
```

### Características clave de MVVM

| Aspecto | Cómo se resuelve |
|---------|-----------------|
| **Estado** | Uno o varios `StateFlow` — el estado está disperso |
| **Eventos** | Lambdas sueltas — no hay estructura formal |
| **Side effects** | Se manejan en el composable o en el Route (navegación inline, snackbars en la View) |
| **Testeabilidad** | Fácil testear el estado (`StateFlow.value`), difícil testear side effects (necesitás la UI) |
| **Type safety** | Las lambdas no fuerzan a manejar todos los casos — si agregás una acción nueva, el compilador no te avisa si no la wireaste |
| **Boilerplate** | Bajo — un `MutableStateFlow` y métodos públicos |

---

## MVI (Model-View-Intent)

### Estructura

```
View → Event → ViewModel → reduce(State, Event) → State → View
                 ↓
            SideEffect (one-shot, no re-ejecutable)
```

El flujo es **estrictamente unidireccional y cíclico**:

1. **View** dispara un `Event` (el usuario hizo algo)
2. **ViewModel** recibe el `Event`, actualiza el `State`, y opcionalmente emite un `SideEffect`
3. **View** se recompone con el nuevo `State`
4. Los `SideEffect` son eventos únicos (navegación, snackbar) que se consumen una vez

### Cómo se ve en código real

```kotlin
// MVI: eventos tipados
sealed interface ArticlesListEvent {
    data class SearchQueryChanged(val query: String) : ArticlesListEvent
    data object ClearSearch : ArticlesListEvent
    data class ArticleClicked(val articleId: Int) : ArticlesListEvent
}

// MVI: efectos secundarios (one-shot)
sealed interface ArticlesListSideEffect {
    data class NavigateToDetail(val articleId: Int) : ArticlesListSideEffect
}

// MVI: estado unificado
data class ArticlesListState(
    val searchQuery: String = "",
    val articles: Flow<PagingData<Article>>? = null,
)

// ViewModel — MVI: un solo entry point
@HiltViewModel
class ArticlesListViewModel @Inject constructor(...) : ViewModel() {

    private val _state = MutableStateFlow(ArticlesListState())
    val state: StateFlow<ArticlesListState> = _state.asStateFlow()

    private val _sideEffects = Channel<ArticlesListSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ArticlesListSideEffect> = _sideEffects.receiveAsFlow()

    fun onEvent(event: ArticlesListEvent) {
        when (event) {
            is ArticlesListEvent.SearchQueryChanged ->
                _state.update { it.copy(searchQuery = event.query) }

            is ArticlesListEvent.ArticleClicked -> {
                analytics.logEvent("article_selected", mapOf(...))
                viewModelScope.launch {
                    _sideEffects.send(ArticlesListSideEffect.NavigateToDetail(event.articleId))
                }
            }
            // ← El compilador obliga a manejar TODOS los casos
        }
    }
}

// View — MVI: estado unificado + un solo handler
@Composable
fun ArticlesListScreen(
    state: ArticlesListState,
    onEvent: (ArticlesListEvent) -> Unit,
) {
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = { onEvent(ArticlesListEvent.SearchQueryChanged(it)) },
    )
}

// Route — MVI: recolecta side effects
@Composable
fun ArticlesListScreenRoute(onNavigate: (Int) -> Unit, vm: ArticlesListViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.sideEffects.collect { effect ->
            when (effect) {
                is ArticlesListSideEffect.NavigateToDetail -> onNavigate(effect.articleId)
            }
        }
    }

    ArticlesListScreen(state = state, onEvent = vm::onEvent)
}
```

### Características clave de MVI

| Aspecto | Cómo se resuelve |
|---------|-----------------|
| **Estado** | Un solo `StateFlow<ScreenState>` — snapshot atómico de toda la pantalla |
| **Eventos** | `sealed interface Event` — el compilador fuerza a manejar todos los casos |
| **Side effects** | `Channel<SideEffect>` + `SharedFlow` emitidos por el ViewModel, consumidos en el Route |
| **Testeabilidad** | Máxima — podés assert sobre `state.value` Y sobre `sideEffects` sin tocar la UI |
| **Type safety** | Si agregás un `Event` nuevo y no lo manejás en el `when`, **no compila** |
| **Boilerplate** | Medio — `sealed interface` por pantalla + canal de side effects |

---

## Comparación directa

### Misma funcionalidad, dos patrones

**Cambiar el texto de búsqueda:**

| | MVVM | MVI |
|---|---|---|
| ViewModel expone | `fun onSearchTextChange(text: String)` | `fun onEvent(SearchQueryChanged(text))` |
| View llama | `actions.onSearchTextChange(text)` | `onEvent(SearchQueryChanged(text))` |
| Estado se actualiza | `_searchQuery.value = text` | `_state.update { it.copy(searchQuery = text) }` |

**Navegar al detalle de un artículo:**

| | MVVM | MVI |
|---|---|---|
| ViewModel | No participa — el Route recibe `onArticleClick: (Int) -> Unit` como callback y navega directo | Emite `NavigateToDetail(id)` via `_sideEffects.send()` |
| Route | Pasa `onArticleClick = { navController.navigate(...) }` directo a las Actions | Colecta `sideEffects` con `LaunchedEffect` y navega cuando recibe `NavigateToDetail` |
| Quién decide navegar | El Route (la capa de navegación) | El ViewModel (la capa de lógica) |

### Tabla comparativa

| | MVVM | MVI |
|---|---|---|
| **Dirección del flujo** | UDF con múltiples canales de vuelta (lambdas sueltas) | UDF con un solo canal de vuelta (`onEvent`) |
| **Entry point del VM** | N métodos públicos (uno por acción) | Un solo método: `fun onEvent(event: Event)` |
| **Estado** | Disperso en N `StateFlow` | Unificado en 1 `StateFlow<ScreenState>` |
| **Eventos de UI** | Lambdas (`() -> Unit`, `(String) -> Unit`) | `sealed interface` tipada |
| **Side effects** | Inline en el composable o Route | `Channel` + `Flow` explícito |
| **Exhaustividad** | No forzada — podés olvidarte de wirear una acción | Forzada por el compilador (`when` exhaustivo) |
| **Serialización del estado** | Hay que serializar cada `MutableStateFlow` | Un solo `ScreenState` → una sola operación |
| **Debugging** | Hay que rastrear qué método público se llamó y cuándo | Todos los eventos pasan por `onEvent()` → un solo lugar para logs/breakpoints |
| **Curva de aprendizaje** | Baja | Media |
| **Líneas de código extra** | 0 | ~40 por feature |

---

## Flujo visual lado a lado

```
MVVM                                    MVI
────                                    ───

  ┌──────────┐                            ┌──────────┐
  │   View   │                            │   View   │
  └────┬─────┘                            └────┬─────┘
       │                                      │
       │ StateFlow<T>                         │ StateFlow<ScreenState>
       ▼                                      ▼
  ┌──────────────┐                      ┌──────────────┐
  │  ViewModel   │                      │  ViewModel   │
  │              │                      │              │
  │  searchQuery │◄──── onTextChange()  │  _state ─────│◄── onEvent(Event)
  │  articles ───│◄──── clearSearch()   │  _sideEffects │
  │  uiState ────│◄──── onArticleClick()│              │
  │              │◄──── sendAnalytics() │              │
  └──────────────┘                      └──────┬───────┘
       │                                      │
       │ Result<T> / Flow<PagingData>         │ Result<T> / Flow<PagingData>
       ▼                                      ▼
  ┌──────────────┐                      ┌──────────────┐
  │  Repository  │                      │  Repository  │
  └──────────────┘                      └──────────────┘
```

En MVVM tenés **N canales de vuelta** (uno por método público). En MVI tenés **1 solo canal de vuelta** (`onEvent`), y los side effects salen por un canal separado.

---

## MVI Lite — El punto medio

No hace falta irse a MVI completo. Existe un punto intermedio ([ver implementación en la rama `mvi-lite`](../mvi-lite)):

```
MVVM + canal de side effects = MVI Lite
```

**Qué cambia:**
- Agregás `sealed interface XxxSideEffect` y un `Channel<SideEffect>` en el ViewModel
- La navegación pasa por side effects en vez de callbacks directos
- El resto (estado disperso, métodos sueltos, Attributes/Actions) se queda igual

**Qué ganás:**
- Navegación testeable sin UI
- Separación de concerns (el ViewModel decide CUÁNDO navegar)
- Sin el boilerplate de MVI completo

**Qué NO cambiás:**
- El estado sigue disperso
- Los eventos siguen siendo lambdas
- Las Screen siguen con Attributes/Actions

---

## ¿Cuándo usar cada uno?

| Escenario | Recomendación |
|-----------|--------------|
| 1-3 pantallas, lógica simple, 1 dev | **MVVM** — no sobre-ingenieriles |
| 3-5 pantallas, navegación con lógica, side effects frecuentes | **MVI Lite** — solo canales de side effects |
| 5+ pantallas, lógica compleja, múltiples devs, necesitás trazabilidad | **MVI completo** — la formalidad paga el boilerplate |
| App financiera, médica o de alta criticidad | **MVI completo** — trazabilidad de eventos y estado predecible son requisitos |
| Prototipo o MVP rápido | **MVVM** — velocidad > formalidad |

---

## Preguntas frecuentes

### ¿MVI reemplaza a MVVM?

No. MVI es una **especialización** de MVVM. Ambos usan ViewModel, StateFlow, UDF. MVI agrega restricciones (eventos tipados, estado unificado, side effects explícitos) que hacen el código más predecible a costa de más boilerplate.

### ¿Paging 3 funciona bien con MVI?

Es el punto de fricción más grande. `Flow<PagingData>` es inherentemente reactivo — no se puede meter en un `reduce()` tradicional. Hay dos soluciones:
1. Mantener `Flow<PagingData>` como campo del `ScreenState` y que la View lo recolecte (el approach que usamos en `mvi-full`)
2. Dejar `Flow<PagingData>` fuera del state y pasarlo como parámetro separado

### ¿Necesito una librería para MVI?

No. `sealed interface` + `MutableStateFlow` + `Channel` es suficiente. Librerías como Orbit MVI o MviKotlin agregan `reduce()` DSL, logging y coroutine scoping, pero no son necesarias para arrancar.

### ¿MVI hace que el estado sea inmutable?

El `ScreenState` es un `data class` inmutable. Cada vez que procesás un evento, creás una copia nueva con `copy()`. Esto es importante porque:
- Podés comparar estados con `==` (útil para testing)
- No hay mutaciones laterales (el estado solo cambia via `onEvent`)
- Podés implementar time-travel debugging (guardar historial de estados)

---

## Ver también

- [[patterns/mvvm-repository]] — Arquitectura MVVM actual del proyecto
- [[patterns/mvi-migration]] — Plan de migración MVVM → MVI, archivo por archivo
- [[patterns/attributes-actions]] — Attributes/Actions pattern
- [[patterns/error-handling]] — Manejo de errores con Result + UiState
- [[patterns/room-paging]] — Room + Paging 3 + RemoteMediator
- `mvi-full` — Rama con MVI completo implementado
- `mvi-lite` — Rama con MVI Lite implementado
