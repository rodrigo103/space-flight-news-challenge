# 🚀 Space Flight News

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=rodrigo103_proyecto-android&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=rodrigo103_proyecto-android)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=rodrigo103_proyecto-android&metric=bugs)](https://sonarcloud.io/summary/new_code?id=rodrigo103_proyecto-android)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=rodrigo103_proyecto-android&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=rodrigo103_proyecto-android)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=rodrigo103_proyecto-android&metric=coverage)](https://sonarcloud.io/summary/new_code?id=rodrigo103_proyecto-android)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=rodrigo103_proyecto-android&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=rodrigo103_proyecto-android)

**SpaceFlightNews** — Android app que consume la [Spaceflight News API](https://api.spaceflightnewsapi.net/v4/) para explorar artículos del mundo aeroespacial. Construida con tecnologías modernas del ecosistema Android, arquitectura limpia y testing sólido.

---

## 📸 Capturas de pantalla

<p align="center">
  <img src="screenshots/phone-list.png" width="30%" alt="Teléfono — Lista" />
  &nbsp;&nbsp;
  <img src="screenshots/tablet-dual.png" width="63%" alt="Tablet — Dual pane" />
  &nbsp;&nbsp;
  <img src="screenshots/phone-error.png" width="30%" alt="Teléfono — Error" />
</p>

---

## ✨ Características destacadas

### 📱 Diseño responsivo y adaptativo

La app se adapta automáticamente al ancho de pantalla del dispositivo:

| Dispositivo            | Layout                                                                 |
| ---------------------- | ---------------------------------------------------------------------- |
| **Teléfono (< 840dp)** | Navegación single-pane con transiciones de slide entre lista y detalle |
| **Tablet (≥ 840dp)**   | Layout dual-pane — lista (40%) + detalle (60%) con divisor vertical    |

El layout se determina dinámicamente con `BoxWithConstraints`, garantizando una experiencia óptima en cualquier dispositivo, desde teléfonos compactos hasta tablets. La selección del artículo persiste a través de rotaciones y cambios de configuración con `rememberSaveable`.

### 🗄️ Caché offline-first con Paging 3

Estrategia de caché a tres niveles que mantiene la app funcional incluso sin conexión:

```
API ──→ RemoteMediator ──→ Room (SQLite) ──→ PagingSource ──→ Flow<PagingData> ──→ UI
```

- **Lista**: paginación infinita desde API, almacenada en Room. Al perder conectividad, la lista sigue mostrando los datos cacheados sin interrupción.
- **Búsqueda**: completamente local contra Room (consulta `LIKE`), sin llamadas a red — instantánea.
- **Detalle**: estrategia remote-first con timeout de 30s; si la red falla, carga desde caché local automáticamente.
- **Banner offline**: `AnimatedVisibility` con slide desde el borde superior cuando se pierde conectividad, monitoreada reactivamente con `ConnectivityObserver`.

### 📊 Analíticas con Firebase

Sistema de analytics con patrón **Composite** que despacha eventos a múltiples backends simultáneamente:

- `FirebaseAnalyticsHelper` → Firebase Analytics (dashboard en consola)
- `TimberAnalyticsHelper` → Logcat (debugging local)

Eventos trackeados: `screen_view`, `article_selected`, `article_loaded`, `app_error`. El `AnalyticsHelper` se inyecta como interfaz, manteniendo el dominio libre de dependencias de Firebase.

![Firebase Analytics Dashboard](screenshots/firebase-analytics.png)

### 🧠 Wiki LLM integrada con OpenCode

Inspirada en la metodología de documentación de Kharpaty, el proyecto incluye una **wiki persistente** en `.opencode/wiki/` que funciona como:

- **Base de conocimiento** incremental que crece con cada decisión de diseño, patrón implementado y problema resuelto.
- **Contexto para LLMs**: OpenCode consume la wiki automáticamente para responder consultas con conocimiento acumulado del proyecto, sin necesidad de re-derivar información.
- **Visualización con Obsidian**: los archivos markdown con cross-links `[[wiki-style]]` se pueden abrir en [Obsidian](https://obsidian.md/) para navegar la documentación como un grafo de conocimiento interactivo.

La wiki del proyecto cubre 5 categorías (arquitectura, patrones, procesos, herramientas, testing) con más de 20 páginas interconectadas.

![Wiki Graph](screenshots/wiki-graph.png)

---

## 🧱 Arquitectura

```
┌─────────────────────────────────────────┐
│  UI Layer (Compose screens + ViewModels) │
├─────────────────────────────────────────┤
│  Domain Layer (models + use cases)       │
├─────────────────────────────────────────┤
│  Data Layer (Repository + API + Room)    │
└─────────────────────────────────────────┘
```

- **MVVM** con `StateFlow<UiState<T>>` — sealed class `Loading | Success<T> | Error`
- **Repository pattern** con `Result<T>` para manejo de errores tipado
- **Attributes/Actions pattern** — screens puras `f(Attributes, Actions) → UI`, altamente testeables
- **Settings+Composable pattern** para sub-componentes reutilizables (cards, banners)

### Stack tecnológico

| Capa | Tecnología |
|---|---|
| **UI** | Jetpack Compose + Material 3 + Lottie Animations |
| **Navegación** | Navigation Compose + type-safe routes |
| **Imágenes** | Coil 3 con OkHttp network fetcher |
| **Red** | Retrofit + OkHttp + kotlinx.serialization |
| **Base de datos** | Room + Paging 3 + RemoteMediator |
| **DI** | Hilt |
| **Analytics** | Firebase Analytics + Timber |
| **Calidad** | Detekt + SonarCloud + LeakCanary |

### Manejo de errores en 4 capas

| Capa | Mecanismo |
|---|---|
| **HTTP** | `HttpErrorCallAdapter` → mapea 4xx/5xx a `ApiException` sellada |
| **Repositorio** | `runCatching` → `Result<T>` |
| **ViewModel** | `onSuccess` / `onFailure` → `UiState<T>` |
| **UI** | Error full-screen (sin datos) / Snackbar (con datos cacheados) + retry |

---

## 🔬 Calidad de código

- **CI/CD** con GitHub Actions: detekt → unit tests → build → instrumented tests en 4 jobs paralelos
- **SonarCloud** para métricas de calidad continua
- **Tests unitarios**: ViewModels con MockK + Turbine, repositorio con MockWebServer
- **Tests instrumentados**: Compose UI tests en emulador
- **Detekt** con reglas custom + reglas específicas de Compose

---

## 🚀 Cómo correr el proyecto

```bash
# Clonar
git clone <repo-url>
cd proyecto-android

# Build
./gradlew assembleDebug

# Tests unitarios
./gradlew test

# Tests instrumentados (requiere emulador)
./gradlew connectedAndroidTest

# Detekt
./gradlew detekt
```

---

## 📂 Estructura del proyecto

```
app/src/main/java/com/example/myandroidapp/
├── analytics/          # AnalyticsHelper + Firebase + Timber (Composite)
├── data/
│   ├── connectivity/   # ConnectivityObserver reactivo
│   ├── local/          # Room DB, DAO, Entity, RemoteMediator
│   ├── mappers/        # Entity ↔ Domain mappers
│   ├── remote/         # Retrofit API, HttpErrorCallAdapter
│   └── repository/     # DefaultArticlesRepository
├── di/                 # Hilt modules (App, Network, DB, Repository, Analytics)
├── domain/
│   ├── model/          # Article, Author, ArticleResponse
│   ├── repository/     # ArticlesRepository interface
│   └── usecase/        # GetArticleUseCase
├── theme/              # Material 3 theme + dynamic colors
└── ui/
    ├── articles/
    │   ├── detail/     # ArticleDetailScreen + ViewModel (phone + tablet)
    │   └── list/       # ArticlesListScreen + ViewModel + search
    ├── common/         # UiState sealed class
    ├── components/     # OfflineBanner, GradientBackground
    ├── navigation/     # ResponsiveApp, NavHost, DualPaneScreen
    └── preview/        # Compose @Previews (Loading, Success, Error)
```
