---
tags:
  - wiki/tool
---

# Testing Strategy

> **Last verified:** 2026-10-17 | **Verified by:** [source] — cobertura migrada a Kover 0.9.11 (ratchet 75) y suite actualizada a 52 tests en `:shared` + 17 en `:app`

## Stack de testing

El stack está **partido en dos** desde la migración a KMP: lo que corre en las dos plataformas y lo que es solo Android. [source]

| Herramienta | Dónde | Uso |
|---|---|---|
| `kotlin.test` | `:shared/commonTest` | Asserts multiplataforma |
| **Ktor MockEngine** | `:shared/commonTest` | HTTP mocking — reemplaza a MockWebServer |
| **Fakes a mano** | `:shared/commonTest` | Dobles de prueba — reemplazan a MockK |
| kotlinx.coroutines.test | ambos | `runTest`, `StandardTestDispatcher` |
| Turbine | `:shared` | Flow testing — secuencias de estado (ver nota) |
| JUnit 4 + MockK | `:app/src/test` | Tests de ViewModel (`SavedStateHandle`) |
| Compose UI Test | `:app/src/androidTest` | Instrumentados |
| Robolectric 4.17 | `:shared/androidHostTest` | Android framework en JVM (`@Config(sdk = [34])`) |

### Por qué cambió

- **MockK no corre en Kotlin/Native.** Todo doble de prueba en `commonTest` se escribe a mano.
- **MockWebServer es JVM-only**, y además `ktor-client-okhttp` arrastra OkHttp 5.x, que rompe `mockwebserver` 4.12 (usa la ya inexistente `okhttp3.internal.Util`).

`mockwebserver` quedó declarado en `app/build.gradle.kts` pero sin usos: los tests de HTTP viven en `:shared`, sobre MockEngine. [source]

### El helper compartido

`testApiService()` monta `MockEngine` con la **misma `configureApiClient()` que usa producción**, así que el mapeo de status codes a `ApiException` que se testea es literalmente el que corre en la app, no una réplica que puede divergir. [analysis]

```kotlin
fun testApiService(
    status: HttpStatusCode = HttpStatusCode.OK,
    body: String = "{}",
    recorded: RecordedRequests? = null,
): ApiService {
    val engine = MockEngine { request ->
        recorded?.urls?.add(request.url.toString())
        respond(ByteReadChannel(body), status, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return ApiService(HttpClient(engine) { configureApiClient() })
}
```

`RecordedRequests` registra las URLs que pasaron por el engine, para poder assertear query params (`limit`, `offset`, `search`). [source]

### Cómo correrlos

```bash
./gradlew :shared:testAndroidHostTest   # commonTest + androidHostTest en JVM — 52 tests
./gradlew :app:testDebugUnitTest        # ViewModels de :app — 17 tests
./gradlew test                          # unit tests de :app (debug + release); NO corre :shared
./gradlew :shared:allTests              # ⚠️ requiere runtime de simulador iOS
```

`allTests` intenta ejecutar en simulador y falla con *"Xcode does not support simulator tests for ios_simulator_arm64"* si no hay runtime instalado (`xcodebuild -downloadPlatform iOS`). Los tests **compilan y linkean** para iOS igual. [gap]

`withHostTest {}` en el bloque `androidLibrary` es lo que habilita correr `commonTest` en JVM; sin eso, la única forma es el simulador. El source set `androidHostTest` (mismo target) aloja tests Android-específicos que corren en JVM: ahí vive `AndroidConnectivityObserverTest` con Robolectric.

## Tipos de tests

### Unit tests (ViewModel)

Prueban el comportamiento del ViewModel con un mock del Repository usando MockK:

```kotlin
class ArticlesListViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `onSearchTextChange updates search query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())
        val analytics = mockk<AnalyticsHelper>(relaxed = true)

        val viewModel = ArticlesListViewModel(repository, analytics)
        viewModel.onSearchTextChange("nasa")

        assertEquals("nasa", viewModel.searchQuery.value)
    }
}
```

### Integration tests (HTTP + Repository)

Ya no usan MockWebServer: el doble es MockEngine con la config de producción, y del lado datos un fake a mano del DAO. Verifican paginación, errores HTTP y fallback offline:

```kotlin
class ArticleRemoteMediatorTest {
    @Test
    fun `refresh on a full page inserts items and keeps pagination open`() = runTest {
        val recorded = RecordedRequests()
        val dao = FakeArticleDao()
        val api = testApiService(body = articlesJson(pageSize), recorded = recorded)

        val result = ArticleRemoteMediator(api, dao).load(LoadType.REFRESH, emptyState())

        assertTrue(result is Success)
        assertEquals(pageSize, dao.inserted.size)
        assertContains(recorded.urls.first(), "offset=0")
    }
}
```

### ViewModel tests with Turbine

Verifican secuencias exactas de estados:

```kotlin
viewModel.description.test {
    assertEquals("", awaitItem())              // Initial
    assertEquals(loadingMessage, awaitItem())   // Loading
    assertEquals(finalDescription, awaitItem()) // Final
    awaitComplete()
}
```

Turbine se usa en `AndroidConnectivityObserverTest` (secuencias con `awaitItem`/`expectNoEvents`). Los tests del pane VM, en cambio, avanzan el dispatcher con `mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()` y leen `uiState.value`. [source]

## Patrones avanzados

Patrones de referencia. Vigentes en `main`: `MainDispatcherRule` y los fakes. Paparazzi y las custom semantics **no** están integrados (no hay dependencia ni usos). [source]

### MainDispatcherRule

El rule real del proyecto (ya no se llama `MainCoroutineTestRule`):

```kotlin
class MainDispatcherRule : TestWatcher() {
    val testDispatcher = StandardTestDispatcher()

    override fun starting(description: Description?) {
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description?) {
        Dispatchers.resetMain()
    }
}
```

### Custom Semantic Properties (UI testing)

Más robusto que `testTag`:

```kotlin
object CustomSemantics {
    val UiState = SemanticsPropertyKey<String>("UiState")
    val Variant = SemanticsPropertyKey<String>("Variant")
}

// En el componente:
Modifier.semantics {
    this[CustomSemantics.UiState] = "loading"
}

// En el test:
composeTestRule.onNode(SemanticsMatcher("is loading") {
    it.config[CustomSemantics.UiState] == "loading"
}).assertExists()
```

### Fakes vs Mocks

| Aspecto | Fakes | Mocks |
|---|---|---|
| Mantenimiento | Escribir una vez | `coEvery { ... } returns ...` en cada test |
| Fragilidad | Baja | Alta (cambios de API rompen mocks) |
| Legibilidad | El fake documenta el comportamiento | Arrange es verboso |
| Costo inicial | Alto | Bajo |

Para proyectos que crecen, los **fakes** son superiores. Ejemplo real: `FakeArticleDao` implementa `ArticleDao` con un `MutableMap` y expone `inserted` para assertear. [source]

### Paparazzi Screenshot Testing

Para detectar regresiones visuales. Captura screenshots en múltiples dispositivos y modos. **No integrado en `main`** — se documenta como camino a futuro:

```kotlin
@Test fun `article list screenshot`() {
    paparazzi.snapshot {
        ArticleListScreen(
            attributes = ArticlesListAttributes(searchQuery = "", articles = sampleFlow),
            actions = noopActions,
        )
    }
}
```

## Cobertura (Kover)

La cobertura la mide **Kover 0.9.11** (`org.jetbrains.kotlinx.kover`), aplicado en `:app` y `:shared`. Reemplazó a JaCoCo: se eliminaron el plugin `jacoco`, el flag `enableUnitTestCoverage` y el task manual `jacocoTestReport`. [source]

El **reporte "total"** — el que consume SonarCloud y el que gatea el CI — está scopeado a la **capa lógica**: packages `com.example.myandroidapp.data` + `com.example.myandroidapp.domain` y clases `*ViewModel*`. Excluye el código generado (`*.R`, `*.BuildConfig`, `*_Impl*`, `*DatabaseConstructor`, `*Preview*`, `ComposableSingletons*`), y el package `di`. La UI Compose no entra en el gate: se valida con tests instrumentados y se inspecciona a mano en `koverHtmlReportDebug`. [source]

El umbral es un **ratchet**: `verify { rule("Logica (data + domain + VMs)") { minBound(75) } }` en `app/build.gradle.kts`. La cobertura lógica actual ronda el 81%; el piso se sube, nunca se baja. El job `unit-tests` del CI corre `./gradlew test :app:koverVerify` y rompe el build si cae por debajo. [source]

| Comando | Qué genera |
|---|---|
| `./gradlew :app:koverVerify` | Aplica el umbral (75) — falla el build si la cobertura lógica no llega |
| `./gradlew :app:koverXmlReport` | XML JaCoCo-compatible (`app/build/reports/kover/report.xml`) para SonarCloud |
| `./gradlew :app:koverHtmlReport` | HTML del reporte "total" (solo lógica) |
| `./gradlew :app:koverHtmlReportDebug` | HTML de `:app` completa, incluye UI Compose |

`kover(project(":shared"))` en las dependencias de `:app` es lo que agrega la cobertura de `:shared` (data + domain) al reporte de `:app`. [source]

### Gotcha: token de SonarCloud

El `SONAR_TOKEN` puede estar vencido (~90 días de vida). Síntoma: el job de `sonar.yml` falla con 403 *"scanner bootstrapping failed"*. Se arregla regenerando el secret en GitHub. [analysis]

### Nota: línea `android-native`

En la línea hermana `android-native` (Android nativo puro, mismo repo) también hay Kover, pero con ratchet **70**, y además tests de DAO con Room in-memory real y MockWebServer en `:app`. En `main`, Robolectric corre en `:shared/androidHostTest`; en ambas líneas el SDK está pinneado a 34 (SDK 36 de Robolectric pide Java 21). [source]

## Archivos de testing en el proyecto

| Archivo | Módulo | Tipo | Prueba |
|---|---|---|---|
| `test/Fakes.kt` | `:shared` | Fixture | `FakeArticleDao`, `FakeArticlesRepository`, `RecordedRequests`, `testApiService` |
| `test/TestArticleData.kt` | ambos | Fixture | Constantes de prueba compartidas |
| `test/TestJson.kt` | `:shared` | Fixture | JSON para MockEngine |
| `data/remote/ApiServiceTest.kt` | `:shared` | Integration | Parser y mapeo de status codes (11) |
| `data/ArticlesRepositoryTest.kt` | `:shared` | Integration | Pipeline completo (13) |
| `data/local/ArticleRemoteMediatorTest.kt` | `:shared` | Integration | Mediator: refresh/append/prepend, search, errores (8) |
| `data/connectivity/AndroidConnectivityObserverTest.kt` | `:shared` (androidHostTest) | Integration | Observer: snapshot, callbacks, dedup, unregister (10) |
| `data/mappers/ArticleMappersTest.kt` | `:shared` | Unit | Mappers Entity ↔ Domain (4) |
| `domain/usecase/GetArticleUseCaseTest.kt` | `:shared` | Unit | Timeout + fallback de cache (6) |
| `test/MainDispatcherRule.kt` | `:app` | Rule | Test CoroutineDispatcher |
| `ui/articles/list/ArticlesListViewModelTest.kt` | `:app` | Unit | ViewModel states (6) |
| `ui/articles/detail/ArticleDetailViewModelTest.kt` | `:app` | Unit | Detail states (6) |
| `ui/articles/detail/ArticleDetailPaneViewModelTest.kt` | `:app` | Unit | Pane states + analytics (5) |

Totales al 2026-10-17: **52** tests en `:shared` (`testAndroidHostTest`) y **17** en `:app` (`testDebugUnitTest`). [source]

## Ver también

- [[processes/build-and-test]] — Cómo compilar y testear
- [[patterns/error-handling]] — Error handling + testing
- [[patterns/kmp-migration]] — Por qué `:shared` es KMP y cómo se testea
