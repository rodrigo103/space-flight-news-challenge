---
tags:
  - wiki/pattern
---

# Analytics con Composite Pattern

> **Last verified:** 2026-05-19 | **Verified by:** [source]

## Arquitectura

El módulo `analytics/` implementa el **Composite Pattern** para despachar eventos de analytics a múltiples backends simultáneamente sin contaminar el código de negocio.

```
ViewModel / Screen
        │
        ▼
   AnalyticsHelper (interface)
        │
        ▼
CompositeAnalyticsHelper
        │
   ┌────┴────┐
   ▼         ▼
Firebase  Timber
```

## Interface `AnalyticsHelper`

```kotlin
interface AnalyticsHelper {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun logError(throwable: Throwable, context: String = "")
    fun logScreenView(screenName: String)
}
```

Tres operaciones: eventos custom, errores con stacktrace, y screen views. Todas fire-and-forget (no suspend).

## Implementaciones

### `TimberAnalyticsHelper`

Loggea a Logcat vía Timber tag `"Analytics"`:

- `logEvent` → `Timber.d("Event: %s | params: %s")`
- `logError` → `Timber.e(throwable, "Error: %s")`
- `logScreenView` → `Timber.d("Screen View: %s")`

Útil en debug para ver el flujo de eventos sin depender de Firebase.

### `FirebaseAnalyticsHelper`

Despacha a `FirebaseAnalytics` SDK:

- `logEvent` → `firebaseAnalytics.logEvent(name) { param(key, value) }`
- `logError` → evento custom `"app_error"` con params `context` y `error`
- `logScreenView` → `FirebaseAnalytics.Event.SCREEN_VIEW` + `Param.SCREEN_NAME`

Requiere `@ApplicationContext` para `FirebaseAnalytics.getInstance(context)`.

### `CompositeAnalyticsHelper`

```kotlin
class CompositeAnalyticsHelper(
    private val delegates: List<AnalyticsHelper>,
) : AnalyticsHelper {
    override fun logEvent(name: String, params: Map<String, String>) {
        delegates.forEach { it.logEvent(name, params) }
    }
    // logError y logScreenView siguen el mismo patrón
}
```

Cada operación itera sobre todos los delegates, despachando a Firebase y Timber simultáneamente.

## DI Binding

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides @Singleton
    fun bindAnalyticsHelper(
        firebase: FirebaseAnalyticsHelper,
        timber: TimberAnalyticsHelper,
    ): AnalyticsHelper = CompositeAnalyticsHelper(listOf(firebase, timber))
}
```

- El orden de la lista determina orden de dispatch (primero Firebase, después Timber).
- Para quitar Firebase en debug, cambiar la implementación sin tocar ViewModels o Screens.

## Uso en ViewModels

```kotlin
@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val analyticsHelper: AnalyticsHelper,
    ...
) : ViewModel() {
    init {
        analyticsHelper.logScreenView("ArticlesList")  // ciclo de vida
    }

    fun sendAnalytics(event: String, properties: Map<String, String>) {
        analyticsHelper.logEvent(event, properties)  // eventos de usuario (via Actions)
    }
}
```

Los ViewModels inyectan `AnalyticsHelper` (la interfaz, no la implementación concreta). `sendAnalytics()` se expone como callback en las Actions para que la Screen dispare eventos de usuario.

## Eventos registrados

| Evento | Dónde | Propiedades |
|---|---|---|
| `screen_view` / `ArticlesList` | `ArticlesListViewModel.init` | — |
| `screen_view` / `ArticleDetail_$id` | `ArticleDetailViewModel.init` / `ArticleDetailPaneViewModel.loadArticle` | — |
| `article_selected` | Screen → onArticleClick | `id` |
| `article_loaded` | ViewModel → onSuccess | `id` |
| `app_error` / `loadArticle_$id` | ViewModel → onFailure | `context`, `error` |

## Por qué Composite y no Strategy

- **Composite**: dispatch a todos los backends simultáneamente. Ideal para debug (Timber visible en Logcat + Firebase en prod).
- **Strategy**: elegir un backend en runtime (ej. Firebase en prod, Timber en debug). Menos flexible para desarrollo donde querés ver ambos.

## Ver también

- [[architecture/di-hierarchy]] — DI con Hilt, AnalyticsModule
- [[patterns/attributes-actions]] — Analytics vía Actions en las pantallas
- [[tools/key-dependencies]] — Firebase Analytics + Timber
