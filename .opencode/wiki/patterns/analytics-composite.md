---
tags:
  - wiki/pattern
---

# Analytics con Composite Pattern

> **Last verified:** 2026-08-05 | **Verified by:** [source] — movido a `:shared` en la Fase 5 de [[patterns/kmp-migration]]

## Arquitectura

El módulo `analytics/` implementa el **Composite Pattern** para despachar eventos de analytics a múltiples backends simultáneamente sin contaminar el código de negocio. Vive en `:shared`, con los delegates que dependen de la plataforma en sus source sets.

```
ViewModel / Screen
        │
        ▼
   AnalyticsHelper (interface, commonMain)
        │
        ▼
CompositeAnalyticsHelper (commonMain)
        │
   ┌────┴──────────────┐
   ▼                   ▼
Firebase           Logging
(androidMain)      (commonMain, vía AppLogger)
```

En iOS el composite lleva **solo** el helper de logging: Firebase requiere su propio SDK del lado Swift. [gap]

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

### `LoggingAnalyticsHelper`

Vive en `commonMain` y loguea vía `AppLogger`, que resuelve a **Timber en Android** y **`NSLog` en iOS** — el mismo helper sirve en ambas plataformas. Reemplazó a `TimberAnalyticsHelper`, que era Android-only. [source]

- `logEvent` → `AppLogger.d("Event: $name | params: $params", "Analytics")`
- `logError` → `AppLogger.e(throwable, "Error: $context", "Analytics")`
- `logScreenView` → `AppLogger.d("Screen View: $screenName", "Analytics")`

`AppLogger` recibe un `tag` opcional justamente para **no degradar el tag de Logcat a un prefijo de mensaje**. Verificado: Logcat sigue mostrando `D Analytics: ...` igual que antes de la migración. [source]

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

Con Koin, en `appModule` (ver [[tools/koin-setup]]):

```kotlin
single<AnalyticsHelper> {
    CompositeAnalyticsHelper(
        listOf(
            FirebaseAnalyticsHelper(androidContext()),
            LoggingAnalyticsHelper(),
        ),
    )
}
```

- El orden de la lista determina orden de dispatch (primero Firebase, después logging).
- Para quitar Firebase en debug, cambiar la lista sin tocar ViewModels ni Screens.
- El `iosApp` armará su propio composite, con solo `LoggingAnalyticsHelper` por ahora.

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
| `article_deleted` | `ArticleDetailViewModel.onDeleteArticle` | `id` |
| `app_error` / `loadArticle_$id` | ViewModel → onFailure | `context`, `error` |

## Por qué Composite y no Strategy

- **Composite**: dispatch a todos los backends simultáneamente. Ideal para debug (Timber visible en Logcat + Firebase en prod).
- **Strategy**: elegir un backend en runtime (ej. Firebase en prod, Timber en debug). Menos flexible para desarrollo donde querés ver ambos.

## Ver también

- [[architecture/di-hierarchy]] — DI con Hilt, AnalyticsModule
- [[patterns/attributes-actions]] — Analytics vía Actions en las pantallas
- [[tools/key-dependencies]] — Firebase Analytics + Timber
