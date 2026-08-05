---
tags:
  - wiki/architecture
---

# DI Hierarchy

> **Last verified:** 2026-08-05 | **Verified by:** [source] — Hilt reemplazado por Koin en la Fase 4 de [[patterns/kmp-migration]]

Jerarquía del grafo de inyección. Desde la migración a KMP son **dos módulos Koin**, uno por cada lado de la frontera multiplataforma. Detalles de configuración en [[tools/koin-setup]].

## Módulos

| Módulo | Ubicación | Declara |
|---|---|---|
| `sharedModule(enableHttpLogging)` | `:shared/commonMain` | `ApiService`, `ArticleDao`, `ArticlesRepository`, `GetArticleUseCase` |
| `appModule` | `:app` | `AppDatabase`, `ConnectivityObserver`, `AnalyticsHelper`, los 3 ViewModels |

## Dependencias entre módulos

```
appModule
 ├── AppDatabase ──────────┐   (necesita Context, por eso vive acá)
 ├── ConnectivityObserver  │
 ├── AnalyticsHelper       │
 └── ViewModels            │
      ↓ get()              │
sharedModule               │
 ├── ApiService            │
 ├── ArticleDao ←──────────┘   get<AppDatabase>().articleDao()
 ├── ArticlesRepository
 └── GetArticleUseCase
```

El único punto donde el grafo compartido depende del consumidor es `AppDatabase`: `sharedModule` la resuelve con `get()` y cada plataforma la registra, porque construirla necesita input propio de cada una (`Context` en Android, ruta de `NSFileManager` en iOS). [analysis]

## Arranque

`MyApplication.onCreate()` llama a `startKoin { }` con `androidContext()` y ambos módulos. Ya no hay `@HiltAndroidApp` ni `@AndroidEntryPoint`.

## Scopes

| Koin | Significado |
|---|---|
| `single { }` | Instancia única por aplicación (equivale a `@Singleton` de Hilt) |
| `factory { }` | Instancia nueva en cada resolución |
| `viewModel { }` | Atado al ciclo de vida del `ViewModelStoreOwner` |

`GetArticleUseCase` es `factory` porque no tiene estado; el resto son `single`.

## ⚠️ El grafo no se valida al compilar

Ver [[tools/koin-setup]]. Es el costo aceptado de esta migración: Dagger verificaba el grafo en tiempo de compilación, Koin resuelve por reflexión en runtime. Un binding faltante pasa el build y los 46 tests, y falla al abrir la app. [gap]

## Ver también

- [[tools/koin-setup]] — Configuración de Koin y equivalencias con Hilt
- [[architecture/app-structure]] — Estructura de módulos
- [[patterns/kmp-migration]] — Plan y estado de la migración
