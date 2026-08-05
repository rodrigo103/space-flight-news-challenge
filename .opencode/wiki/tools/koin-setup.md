---
tags:
  - wiki/tool
---

# Koin Setup

> **Last verified:** 2026-08-05 | **Verified by:** [source] — Hilt reemplazado por Koin en la Fase 4 de [[patterns/kmp-migration]]

Koin 4.2.2 reemplazó a Hilt porque **Hilt es Dagger sobre Android y no compila para Kotlin/Native**. Ver [[tools/hilt-setup]] para la configuración anterior.

## Estructura del grafo

Dos módulos, uno por cada lado de la frontera multiplataforma:

```
sharedModule(enableHttpLogging)   ← :shared/commonMain — capa data
appModule                         ← :app — Android: DB, analytics, conectividad, ViewModels
```

### `sharedModule` — `:shared/commonMain`

```kotlin
fun sharedModule(enableHttpLogging: Boolean = false) = module {
    single { createApiService(enableLogging = enableHttpLogging) }
    single { get<AppDatabase>().articleDao() }
    single<ArticlesRepository> { DefaultArticlesRepository(get(), get()) }
    factory { GetArticleUseCase(get()) }
}
```

Dos decisiones deliberadas [analysis]:

- **El flag de logging es un parámetro de función, no un qualifier.** Es un solo valor; así el módulo no depende de que el consumidor registre un `named()` escrito igual.
- **`AppDatabase` no se declara acá.** Construirla necesita input de plataforma (`Context` en Android, ruta de `NSFileManager` en iOS), así que cada app la registra y `sharedModule` la resuelve con `get()`. Es el único punto donde el grafo compartido depende de que el consumidor aporte algo.

### `appModule` — `:app`

```kotlin
val appModule = module {
    single { createDatabase(androidContext()) }
    single { ConnectivityObserver(androidContext()) }
    single<AnalyticsHelper> {
        CompositeAnalyticsHelper(listOf(FirebaseAnalyticsHelper(androidContext()), TimberAnalyticsHelper()))
    }
    viewModel { ArticlesListViewModel(get(), get()) }
    viewModel { ArticleDetailPaneViewModel(get(), get()) }
    viewModel { ArticleDetailViewModel(get(), get(), get()) }
}
```

`SavedStateHandle` lo inyecta Koin **automáticamente** por estar en el constructor de `ArticleDetailViewModel` — no hace falta DSL especial. Las funciones `stateViewModel` están deprecadas. [source]

## Arranque

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebug = isDebugBuild(applicationInfo)
        if (isDebug) Timber.plant(Timber.DebugTree())

        startKoin {
            if (isDebug) androidLogger(Level.ERROR)
            androidContext(this@MyApplication)
            modules(sharedModule(enableHttpLogging = isDebug), appModule)
        }
    }
}
```

Ya no hay `@HiltAndroidApp` ni `@AndroidEntryPoint`. `MainActivity` obtiene sus dependencias con `by inject()`, y los composables con `koinViewModel()`.

## Equivalencias con Hilt

| Hilt | Koin |
|---|---|
| `@HiltAndroidApp` | `startKoin { }` en `onCreate` |
| `@AndroidEntryPoint` | nada — `by inject()` directo |
| `@Module @InstallIn(SingletonComponent::class)` | `val x = module { }` |
| `@Provides @Singleton` | `single { }` |
| `@Provides` (sin scope) | `factory { }` |
| `@Binds` interfaz→impl | `single<Interfaz> { Impl(get()) }` |
| `@HiltViewModel` + `@Inject constructor` | `viewModel { }` en el módulo |
| `hiltViewModel()` | `koinViewModel()` |
| `@ApplicationContext Context` | `androidContext()` |
| `@Named("x")` / `@Qualifier` | `named("x")` |

## ⚠️ El costo: el grafo ya no se valida al compilar

**Es la contrapartida real de esta migración.** Dagger/Hilt verificaban el grafo completo en tiempo de compilación: una dependencia faltante rompía el build. Koin resuelve por reflexión en runtime, así que **un binding faltante crashea al abrir la app**. [analysis]

Agravante en este proyecto: los tests de ViewModel de `:app` construyen los ViewModels **a mano**, así que no ejercitan Koin. Un grafo roto pasa `assembleDebug`, pasa los 46 tests, y falla recién al iniciar.

`verify()` de `koin-test` no ayuda mucho acá: inspecciona firmas de constructores y no puede mirar dentro de los lambdas del DSL, que es como está escrito este grafo.

**Mitigación práctica:** después de tocar el grafo, correr la app. Un smoke test instrumentado que arranque `MainActivity` cubriría el 90% del riesgo con poco costo. [gap]

## Dependencias

```toml
koin = "4.2.2"
koin-core = { module = "io.insert-koin:koin-core" }              # :shared commonMain (api)
koin-android = { module = "io.insert-koin:koin-android" }        # :app
koin-androidx-compose = { module = "io.insert-koin:koin-androidx-compose" }  # :app
```

`koin-core` se expone con `api()` desde `:shared` porque los consumidores declaran módulos contra su DSL.

## Ver también

- [[patterns/kmp-migration]] — Plan y estado de la migración
- [[architecture/di-hierarchy]] — Jerarquía del grafo
- [[tools/hilt-setup]] — Configuración anterior (obsoleta)
