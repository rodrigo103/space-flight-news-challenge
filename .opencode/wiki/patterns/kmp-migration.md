---
tags:
  - wiki/pattern
  - architecture
  - kmp
---

# KMP Migration

> **Last verified:** 2026-08-04 | **Verified by:** [analysis] — plan basado en docs oficiales de Room KMP, Ktor y SKIE + lectura del código actual. Fase 0 (matriz de versiones) verificada y sin bloqueos.

## Contexto

Plan para migrar el proyecto de Android puro a **Kotlin Multiplatform**, compartiendo la lógica de negocio entre Android e iOS y manteniendo **UI nativa en cada plataforma** (Compose en Android, SwiftUI en iOS).

Este documento reemplaza la sección especulativa "Phase 3 — KMP feature-based" de [[architecture/app-structure]], que asumía SQLDelight y Ktor para todo. La investigación contra docs actuales cambió dos decisiones de fondo: **Room ahora soporta KMP oficialmente** (no hace falta SQLDelight) y **la UI no se comparte** (decisión de producto, no técnica).

### Decisiones tomadas

| Decisión | Elección | Por qué |
|---|---|---|
| Qué se comparte | Solo lógica (domain + data) | UI nativa en cada plataforma; los ViewModels quedan por plataforma |
| UI de iOS | SwiftUI | Look & feel nativo; las screens Compose actuales no se reutilizan en iOS |
| DI | Koin en todo el proyecto | Hilt no compila para Native; una sola historia de DI |
| Tests | Migrar a `commonTest` con fakes | Única forma de testear la lógica compartida en ambas plataformas |
| Base de datos | Room KMP (no SQLDelight) | Room 2.8+ soporta KMP — DAOs y entidades sobreviven [source] |
| HTTP | Ktor plano | Ktorfit es la alternativa si se quiere conservar la sintaxis anotada de Retrofit |

---

## Arquitectura objetivo

```
:shared (KMP)  ──►  domain + data     ← androidTarget, iosX64, iosArm64, iosSimulatorArm64
      │
      ├──► :app (Android)   Compose + ViewModels actuales, DI con Koin
      └──► iosApp (Swift)   SwiftUI + ObservableObjects, consume :shared vía SKIE
```

La presentación **no** se comparte. Android mantiene sus ViewModels (`ArticlesListViewModel`, `ArticleDetailViewModel`, `ArticleDetailPaneViewModel`); iOS tiene los suyos en Swift como `ObservableObject`. Es lo coherente con "lógica compartida + UI nativa" y evita el puente más frágil de KMP: exponer ViewModels de AndroidX a Swift. [analysis]

---

## Los tres problemas de interoperabilidad Kotlin ↔ Swift

Son el corazón técnico de la migración. Sin resolverlos, el framework compila pero es inusable desde Swift.

### 1. `kotlin.Result` no se exporta al framework ObjC

`Result` es una **value class**, y las value classes no se exportan a Objective-C/Swift. Toda la interfaz `ArticlesRepository` la devuelve hoy [source]:

```kotlin
suspend fun getArticles(limit: Int = 20, offset: Int = 0): Result<List<Article>>
suspend fun getArticle(id: Int): Result<Article>
```

**Solución:** sealed class propia en `commonMain`, que SKIE convierte en enum exhaustivo de Swift preservando la jerarquía `ApiException`:

```kotlin
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiException) : ApiResult<Nothing>
}

// Puente para que el código Android existente cambie lo mínimo
fun <T> ApiResult<T>.toResult(): Result<T> = when (this) {
    is ApiResult.Success -> Result.success(data)
    is ApiResult.Failure -> Result.failure(error)
}
```

### 2. `PagingData` no es consumible desde SwiftUI

`paging-common` sí es KMP y compila para iOS, pero **no existe equivalente de `collectAsLazyPagingItems()` en SwiftUI**. Intentar consumir `Flow<PagingData<Article>>` desde Swift no es viable. [analysis]

**Solución:** `getArticlesPaged()` se queda en shared y la usa solo Android. iOS usa el `getArticles(limit, offset)` que **ya existe** en la interfaz — no hay que inventar API nueva.

### 3. `Flow` y `suspend` desde Swift

Los resuelve **SKIE** (plugin del compilador de Kotlin/Native, de Touchlab) [source]:

- `Flow`, `StateFlow` y `MutableStateFlow` → clases Swift que implementan `AsyncSequence`, preservando el tipo genérico
- Funciones `suspend` → `async`/`await` de Swift, con cancelación bidireccional (cancelar el `Task` cancela la corrutina)
- Sealed classes → enums exhaustivos de Swift

---

## Bloqueadores en el código actual

Rompen la compilación para iOS. No son opcionales. [source]

| Archivo | Problema | Solución |
|---|---|---|
| `domain/build.gradle.kts` | `javax.inject` es JVM-only | Eliminar (Koin no lo necesita) |
| `data/local/ArticleRemoteMediator.kt:10-12` | Captura `java.net.UnknownHostException`, `ConnectException`, `SocketTimeoutException` | Excepciones comunes de Ktor |
| `data/repository/DefaultArticlesRepository.kt:16` | `Timber` es JVM-only | Logger común (Napier/Kermit) o `expect/actual` |
| `data/remote/HttpErrorCallAdapter.kt` | `java.lang.reflect`, Retrofit, OkHttp | `HttpResponseValidator` de Ktor, mismo mapeo status → `ApiException` |
| `data/remote/ApiService.kt` | Interfaz Retrofit anotada | Cliente Ktor |
| `data/connectivity/ConnectivityObserver.kt` | `ConnectivityManager` de Android | `expect/actual`: Android igual, iOS con `NWPathMonitor` |
| `analytics/FirebaseAnalyticsHelper.kt` | SDK de Firebase Android | Interfaz común; impl Firebase en `androidMain`, iOS en Swift o no-op |

> **Nota:** `:domain` importaba `androidx.paging.PagingData` y `javax.inject`. Lo primero es KMP-compatible (`paging-common` 3.3.5 publica variantes `iosArm64`, `iosX64` y `iosSimulatorArm64`, verificado en su Gradle module metadata); lo segundo no. Resuelto en la Fase 1. [source]

---

## Fases

Cada fase deja el proyecto compilando y verificable.

### Fase 0 — Prerequisitos y matriz de versiones ✅ VERIFICADA

Era el paso de mayor riesgo porque SKIE es un plugin del compilador de Kotlin/Native y es estricto con la versión de Kotlin. **Verificado el 2026-08-04 contra la doc en vivo: no hay bloqueo.** [source]

| Componente | Estado |
|---|---|
| **SKIE 0.10.14** (release 2026-07-27) | Soporta Kotlin **2.0.0 → 2.4.10**. El proyecto está en 2.3.20 → **compatible** |
| Room KMP | 2.8.4 + `sqlite-bundled` 2.6.2 (versiones de la doc oficial de Room KMP) |
| **Ktor 3.5.2** | **Mínimo utilizable acá.** Ver el gotcha de dexing más abajo — 3.2.0 no compila con `minSdk 24` |
| Koin | 4.1.0 (Fase 4) |

### Gotchas de versiones — leer antes de fijar cualquier dependencia

Los dos incidentes de esta migración tuvieron la misma causa raíz: **los índices de búsqueda de paquetes devuelven datos desactualizados**. Ambos costaron tiempo real y uno casi hace abortar el plan entero. [source]

| Incidente | Qué decía el índice | Qué decía la fuente real | Consecuencia si se hubiera creído |
|---|---|---|---|
| SKIE ↔ Kotlin | ctx7: "compatible hasta Kotlin 2.3.10" | `skie.touchlab.co/intro` en vivo: "hasta **2.4.10**" | Abortar el plan o bajar Kotlin sin necesidad |
| Ktor última versión | `search.maven.org/solrsearch`: `latestVersion` = **3.2.0** | `maven-metadata.xml`: `<release>` = **3.5.2** | Quedarse en una versión que no dexea (ver abajo) |

**Regla:** para versiones, ir siempre a la fuente autoritativa, no al índice.

```bash
# Maven Central / Google Maven — el metadata no miente
curl -s https://repo1.maven.org/maven2/io/ktor/ktor-client-core/maven-metadata.xml | grep -E "<release>|<latest>"

# ¿Publica variantes iOS? El Gradle module metadata lo dice
curl -s https://dl.google.com/dl/android/maven2/androidx/room/room-runtime/2.8.4/room-runtime-2.8.4.module | grep -o "iosArm64"
```

Ese último chequeo se usó para validar `paging-common`, `room-runtime` y `room-paging` antes de escribir código — sin él, la Fase 1 y la 3 habrían fallado recién al linkear.

> **Nota:** `search.maven.org/solrsearch` además ordena las versiones como *strings*, así que `0.10.4` le aparece por encima de `0.10.12`. No sirve ni para listar.

Falta: rama `feat/kmp-migration` y descarga del runtime de simulador (`xcodebuild -downloadPlatform iOS`).

### Fase 1 — Módulo `:shared` ✅ COMPLETADA (2026-08-04)

Crear el módulo KMP con los 4 targets, mover `:domain` a `commonMain`, sacar `javax.inject`.

**Hecho** [source]:

- `shared/build.gradle.kts` con `kotlin-multiplatform` + `com.android.kotlin.multiplatform.library` (el plugin recomendado con AGP 9; el viejo `com.android.library` está deprecado para KMP) y bloque `androidLibrary {}`
- 4 targets; los iOS producen `Shared.framework` (`isStatic = true`)
- Fuentes movidas con `git mv` (historial preservado): `domain/src/main/java/...` → `shared/src/commonMain/kotlin/...`
- `:domain` eliminado; `settings.gradle.kts` y `app/build.gradle.kts` apuntan a `:shared`
- `paging-common` declarado con `api(...)`, no `implementation` — `ArticlesRepository` expone `Flow<PagingData<Article>>` en su API pública y `:app` necesita `PagingData` en el classpath de compilación
- `javax.inject` eliminado de `GetArticleUseCase`; puente `@Provides` en `AppModule` hasta la Fase 4

**Verificado:** `:app:assembleDebug` ✅ · `linkDebugFrameworkIosSimulatorArm64` ✅ · 40 tests ✅ · detekt ✅

#### Confirmación empírica del problema de `kotlin.Result`

El header ObjC generado confirma —y agrava— lo documentado arriba. `getArticle(id): Result<Article>` se exporta así: [source]

```objc
- (void)getArticleId:(int32_t)id
    completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler;
```

El tipo de retorno se borró a **`id`** (objeto ObjC sin tipo → `Any?` en Swift). No se pierde solo el `Result`: se pierde también `Article`. **Y compila sin errores ni warnings**, así que el problema no aparece hasta que se escribe el Swift. Refuerza que `ApiResult` (Fase 2) no es cosmético sino necesario. [analysis]

> **Deuda conocida:** detekt no está aplicado a `:shared` — `./gradlew detekt` solo analiza `:app`. Resolver en la Fase 8.

### Fase 2 — Red: Retrofit → Ktor

Cliente en `commonMain`; engine OkHttp en `androidMain`, **Darwin** en `iosMain` [source]. `HttpResponseValidator` reemplaza al `CallAdapter` con el mismo mapeo de status codes. Introducir `ApiResult`.

#### ⚠️ Ktor < 3.5.2 no dexea con `minSdk 24`

Con Ktor **3.2.0** el build de Android falla en `mergeExtDexDebug` [source]:

```
D8: Space characters in SimpleName 'use streaming syntax' are not allowed prior to
    DEX version 040 (field name `use streaming syntax` on class io.ktor.client.plugins.Messages)
```

Ktor declara un campo cuyo nombre lleva espacios (legal en Kotlin con backticks), y los identificadores con espacios requieren **DEX 040, o sea `minSdk` 30+**. Con `minSdk 24` no hay configuración de Gradle que lo arregle: o se sube el `minSdk` —inaceptable— o se cambia de versión de Ktor. **3.5.2 no tiene el problema.**

El mensaje de error solo aparece con `--stacktrace`; sin eso, Gradle dice apenas "Error while dexing" y manda a investigar por el lado equivocado (desugaring, JVM target).

#### Efecto colateral: Ktor rompe MockWebServer

`ktor-client-okhttp` depende de **OkHttp 5.x**, y la resolución de conflictos de Gradle arrastra ahí a todo el grafo — incluido `mockwebserver` 4.12.0, que llama a `okhttp3.internal.Util`, clase que OkHttp 5 eliminó. Resultado: `NoClassDefFoundError` antes de que corra cualquier assertion. [source]

```
com.squareup.okhttp3:okhttp:5.3.2
  Selection reasons: By conflict resolution: between versions 5.3.2, 4.12.0 and 3.14.9
  \--- io.ktor:ktor-client-okhttp-jvm \--- project :shared
```

No se repara en el lugar: esos tests son justamente los que esta fase reemplaza por `MockEngine`. Pero implica que **introducir Ktor y reescribir los tests de red tienen que ir en la misma tanda**, o el repo queda en rojo en el medio.

```kotlin
HttpClient(engine) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    HttpResponseValidator {
        // status → ApiException.BadRequest / Unauthorized / NotFound / Conflict / ServerError
    }
}
```

### Fase 3 — Persistencia: Room → Room KMP

Room soporta KMP desde 2.8 [source]. **Los DAOs, entidades y el RemoteMediator sobreviven casi intactos** — el mayor ahorro respecto de migrar a SQLDelight.

```toml
room = "2.8.4"
sqlite = "2.6.2"
```

```kotlin
// commonMain
@Database(entities = [ArticleEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>

fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder.setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
```

KSP por target: `kspAndroid`, `kspIosArm64`, `kspIosX64`, `kspIosSimulatorArm64`. Plugin `androidx.room` + `androidx.sqlite:sqlite-bundled`.

### Fase 4 — DI: Hilt → Koin

Los 6 módulos Hilt ([[tools/hilt-setup]]) pasan a módulos Koin. `MyApplication` arranca `startKoin`; los ViewModels Android pasan a `koinViewModel()`. Se pierde la verificación del grafo en tiempo de compilación que daba Dagger. [analysis]

### Fase 5 — Abstracciones de plataforma

`expect/actual` para conectividad (`NWPathMonitor` en iOS), logging y analytics.

### Fase 6 — Tests a `commonTest`

MockK no corre en Native y MockWebServer es solo JVM. Se reescriben con **fakes a mano + `MockEngine` de Ktor + Turbine** (que sí es KMP) [source]. Los 14 archivos actuales se migran; los instrumentados de Compose se quedan en `:app`. Ver [[tools/testing-strategy]].

### Fase 7 — `iosApp`

Proyecto SwiftUI, SKIE configurado, un `ObservableObject` por pantalla consumiendo `:shared`. Lista + detalle con paridad funcional.

### Fase 8 — CI, detekt y wiki

Jobs de iOS en CI, detekt para los nuevos source sets, y actualizar las páginas que quedan obsoletas: [[architecture/app-structure]], [[architecture/data-layer]], [[architecture/di-hierarchy]], [[tools/retrofit-setup]], [[tools/hilt-setup]], [[patterns/room-paging]], [[tools/testing-strategy]].

---

## Verificación end-to-end

```bash
./gradlew :app:assembleDebug
./gradlew :shared:allTests                              # commonTest en JVM Y en simulador iOS
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew detekt
# + build del iosApp desde Xcode
```

Paridad funcional en Android: lista, búsqueda, detalle, offline, tablet.

---

## Riesgos

1. ~~**Matriz de versiones (Fase 0)**~~ — **Resuelto 2026-08-04**: SKIE 0.10.14 soporta Kotlin 2.0.0–2.4.10, y el proyecto está en 2.3.20. Ya no bloquea. [source]
2. **Alcance real** — es una reescritura completa de las capas data y DI, no un refactor incremental. La Fase 6 sola son 14 archivos de test. **Este pasa a ser el riesgo principal.**
3. **Paging solo en Android** — la lista de iOS usa paginación por offset manual, sin la infraestructura de Paging 3.
4. **Firebase en iOS** — requiere SDK aparte del lado Swift, o aceptar no-op.

## Estado del entorno

[source] verificado 2026-08-04:

- Xcode 26.6, SDK iOS 26.5, `xcode-select` apuntando a `Xcode.app`, licencia aceptada
- **Sin runtimes de simulador instalados** — el código iOS compila pero no corre hasta `xcodebuild -downloadPlatform iOS`
- Kotlin 2.3.20, AGP 9.2.1, Room 2.7.1, Paging 3.3.5, JDK 21

## Ver también

- [[architecture/app-structure]] — Estructura actual y roadmap de modularización
- [[architecture/data-layer]] — Data layer que se migra
- [[patterns/room-paging]] — Room + Paging 3 + RemoteMediator
- [[tools/retrofit-setup]] — Capa HTTP que reemplaza Ktor
- [[tools/hilt-setup]] — DI que reemplaza Koin
- [[tools/testing-strategy]] — Testing actual, base de la Fase 6
