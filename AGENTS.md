# Proyecto Android — Space Flight News App

Proyecto **Kotlin Multiplatform**: lógica compartida entre Android e iOS, UI nativa en cada plataforma.

```
:shared (KMP)  ──►  domain + data + analytics + connectivity
      │             androidTarget, iosX64, iosArm64, iosSimulatorArm64
      └──►  :app    UI Compose, tema, arranque
```

Migración en curso. Plan y estado en `.opencode/wiki/patterns/kmp-migration.md`.

## Ramas del repo

El repo mantiene dos líneas vivas en paralelo:

| Rama | Rol | Base |
|---|---|---|
| `main` | Troncal KMP: `:app` (Android) + `:shared` (KMP, Koin) | — |
| `feat/ios-app` | WIP app iOS (SKIE + `iosApp/`) sobre `main` | `main` |
| `android-native` | Línea Android nativo puro: `:app` + `:domain`, Hilt, sin KMP | tag `pre-kmp` (aa5080a) |

Reglas:

- **Nunca merge entre líneas** (divergen en módulos y DI): los cambios compartidos se portan con `git cherry-pick -x <sha>`, adaptando si tocan data layer o DI.
- **Desarrollo dual:** toda feature nueva se implementa en ambas líneas; en el mensaje de commit referenciá el espejo (`espejo de android-native@abc1234`).
- Para que los portes apliquen limpios, mantené los mismos paths en la capa UI (`app/src/main/java/.../ui/...` coincide en ambas líneas).

## Build & Test

- `./gradlew assembleDebug` — build de debug
- `./gradlew :app:test` — tests de ViewModel (Android)
- `./gradlew :shared:testAndroidHostTest` — tests de `commonTest` en JVM
- `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` — verifica que iOS linkea
- `./gradlew detekt` — cubre ambos módulos
- Usar `./gradlew` wrapper, no gradle global.

> `./gradlew test` **no** corre los tests de `:shared`, y `:shared:allTests` falla sin un runtime de
> simulador iOS instalado (`xcodebuild -downloadPlatform iOS`).

## Arquitectura

- MVVM con `ViewModel` + `StateFlow<UiState>`. Los ViewModels viven en `:app`: la presentación **no** se comparte.
- `UiState` es una `sealed interface` con `Loading`, `Success<T>`, `Error`.
- **El repositorio devuelve `ApiResult<T>`, no `kotlin.Result`.** `Result` es una value class y no se exporta al framework de Objective-C: el tipo se borra a `Any?` en Swift.
- **DI con Koin**, no Hilt (Hilt es Dagger sobre Android y no compila para Native). Dos módulos: `sharedModule()` en `:shared` y `appModule` en `:app`.
- **HTTP con Ktor**, no Retrofit. Errores vía `HttpResponseValidator` → `ApiException`.
- **Room KMP** con `BundledSQLiteDriver`. Paging 3 con `RemoteMediator` para API + Room.

## Convenciones

- Kotlin con `jvmToolchain(17)`.
- Serialización con `kotlinx.serialization`, no Gson.
- Room + coroutines, sin RxJava.
- Analytics vía `AnalyticsHelper` + `CompositeAnalyticsHelper` (Firebase en Android, logging en ambas).
- Logging vía `AppLogger` (`expect/actual`), **no Timber directo** en código compartido.
- Nada en `:shared` puede usar `javax.inject`, `java.*` ni Timber: no compilan para Native.

## Cuidado con las versiones de dependencias

Los índices de búsqueda de paquetes devuelven datos desactualizados y ya causaron dos incidentes en
este proyecto. Consultar siempre la fuente autoritativa:

```bash
curl -s https://repo1.maven.org/maven2/<path>/maven-metadata.xml | grep -E "<release>"
```

Para saber si una librería publica variantes iOS, mirar su Gradle module metadata (`.module`).
Detalle en `.opencode/wiki/patterns/kmp-migration.md`.

---

# Dos fuentes de verdad: wiki y OpenSpec

El proyecto tiene **dos** sistemas de documentación, y no son redundantes. Antes de escribir en
alguno, verificá cuál corresponde:

| | Rol | Naturaleza |
|---|---|---|
| `.opencode/wiki/` | **Descriptivo** — cómo está construido y **por qué**: arquitectura, decisiones, tradeoffs, gotchas | Acumula; se actualiza *después* del trabajo |
| `openspec/specs/` | **Normativo** — **qué** debe hacer el sistema: requisitos por capacidad | Se reemplaza al archivar un change |

**Regla práctica:**

- ¿Estás documentando una decisión, un tradeoff o algo que te hizo perder una tarde? → **wiki**
- ¿Estás definiendo comportamiento que el sistema debe cumplir? → **spec de OpenSpec**
- ¿Un cambio con requisitos de comportamiento nuevos? → `/opsx:propose`, y leé el wiki primero
- ¿Un refactor o port sin cambio de comportamiento? → **no necesita change de OpenSpec**; el delta
  de spec sería vacío. Las fases 1–5 de la migración KMP entran en esta categoría.

---

# LLM Wiki

El proyecto tiene un wiki persistente en `.opencode/wiki/` que opencode mantiene incrementalmente.

## Cuándo leer el wiki — ¡OBLIGATORIO!

**Siempre, sin excepción, leé la wiki ANTES de responder cualquier consulta.**
Esto aplica incluso para tareas de código — consultá la página relevante antes de editar.

Categorías cubiertas:
- Arquitectura del proyecto (módulos, DI, capas)
- Procesos (build, test, PR workflow)
- Patrones (MVVM, error handling, Room + Paging)
- Herramientas (Retrofit, Hilt, Coil)

Hacer `/wiki search <tema>` o leer directamente en `.opencode/wiki/`.

## Cuándo escribir al wiki

Después de resolver un problema no trivial, compilar conocimiento al wiki si se trata de:
- Un proceso completo (build, deploy, release)
- Arquitectura o estructura del proyecto
- Un patrón reusable o gotcha descubierto
- Configuración de herramientas

**IMPORTANTE**: Después de cambios significativos (arquitectura, API contracts, DI, error handling, nuevas dependencias), **revisá si las páginas del wiki necesitan actualizarse**. Buscá archivos mencionados en el wiki (`grep` de nombres de archivo en `.opencode/wiki/`). Si encontrás referencias a código que cambió o se eliminó, actualizá la página y agregá entrada en `log.md`.

El skill `check-commit` incluye un paso de wiki review antes del commit.

Hacer `/wiki ingest` para procesar archivos en `raw/` o `/wiki ingest <topic>` para compilar conocimiento desde el codebase.

## Formato de páginas

Toda página debe tener:

```markdown
# Título

> **Last verified:** YYYY-MM-DD | **Verified by:**

Contenido con claim types...
```

### Claim types

| Tag | Uso |
|-----|-----|
| `[source]` | Verificado del código o docs |
| `[analysis]` | Conclusión de evidencia |
| `[unverified]` | Suposición sin chequear |
| `[gap]` | Desconocido conocido |

### Cross-links

Usar `[[ruta/sin-extension]]` para enlazar páginas.

### Lint

`/wiki lint` chequea: links rotos, páginas huérfanas, info stale (>30 días), `[unverified]` abundantes.

## Estructura del wiki

```
.opencode/wiki/
├── raw/              # Inbox — drop files aquí
├── index.md          # TOC auto-mantenido
├── log.md            # Changelog cronológico
├── architecture/     # Estructura del app, DI, data layer
├── processes/        # Build, test, PR workflow
├── patterns/         # MVVM, error handling, Room+Paging
└── tools/            # Retrofit, Hilt, dependencias clave
```