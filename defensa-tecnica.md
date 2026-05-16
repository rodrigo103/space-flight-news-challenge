# Guía de Defensa Técnica — Space Flight News App

## Stack resumen

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| DI | Hilt + KSP |
| Red | Retrofit + OkHttp + kotlinx.serialization |
| Imágenes | Coil 3 |
| Estado | StateFlow + UiState data classes |
| Testing | JUnit 4 + MockK + MockWebServer |
| Memoria | LeakCanary (debug only) |
| Logging | OkHttp logging condicional (BODY en debug, BASIC en release) |
| Errores HTTP | `Response<T>` con `extractBody()` |

---

## 1. ¿Por qué Hilt y no Dagger vanilla?

**Hilt ES Dagger.** Hilt es una capa sobre Dagger que elimina boilerplate. Cuando decís "uso Hilt", estás diciendo que entendés Dagger pero además sabés que Google recomienda no escribirlo a mano.

**Puntos para defender:**

1. *"Hilt simplifica el setup sin perder poder de Dagger. En vez de armar `@Component`, `@Subcomponent`, builders y módulos manuales, uso `@HiltAndroidApp` y `@InstallIn`. Es menos código, menos errores, misma capacidad."*

2. *"En un challenge de 2 pantallas, Dagger vanilla es over-engineering. Pero saber que existe y que Hilt se apoya en él muestra que entiendo la diferencia."*

3. *"Si el proyecto escala a más módulos o features, Hilt escala igual que Dagger porque es el mismo motor."*

4. *"Google recomienda Hilt explícitamente en la documentación oficial. Usarlo muestra que sigo buenas prácticas actuales."*

**Si preguntan por KSP vs KAPT:**

- *"Usé KSP en vez de KAPT porque es más rápido, tiene mejor soporte en AGP 9 (built-in Kotlin support) y es el reemplazo natural de KAPT. Dagger Hilt 2.50+ lo soporta."*

---

## 2. ¿Por qué Navigation Compose y no navigation3?

navigation3 es experimental y no está listo para producción. Tuve problemas concretos con él (SavedStateHandle no recibía argumentos, crash al instanciar ViewModels), lo que confirmó que era una mala elección.

**Respuesta sólida:**

- *"Usé Navigation Compose con type-safe routes (`@Serializable` data classes). Es la API estable y recomendada por Google para navegación en Compose."*
- *"Los argumentos se pasan con tipos seguros: `navController.navigate(DetailRoute(articleId))`. No hay strings mágicas ni errores en runtime."*
- *"El `SavedStateHandle` se integra naturalmente con `@HiltViewModel` y los argumentos de navegación."*

---

## 3. Arquitectura: MVVM + Repository Pattern

```
┌─────────────────────────────────────────────────────┐
│  Screen (Composable)                                │
│   └── hiltViewModel() → ViewModel                    │
│         └── @Inject constructor(repository)          │
│               └── ArticlesRepository (interfaz)      │
│                     └── DefaultArticlesRepository    │
│                           └── ApiService (Retrofit)  │
└─────────────────────────────────────────────────────┘
```

**Puntos clave:**

1. *"Cada capa tiene una única responsabilidad y depende de interfaces, no implementaciones concretas."*
2. *"El ViewModel no sabe si los datos vienen de la red o de caché. Solo conoce `ArticlesRepository`."*
3. *"Los `UiState` son `data class` imutables. El ViewModel los expone como `StateFlow` y la UI reacciona a cambios."*
4. *"No hay estados imposibles: `isLoading=true` con `error!=null` nunca ocurre porque el ViewModel los actualiza atómicamente con `update {}`."*

---

## 4. Manejo de errores

**Dos niveles:**

| Nivel | Mecanismo |
|---|---|
| Developer | `Log.e(TAG, ...)` con contexto del error + interceptor de OkHttp para debugging |
| Usuario | Snackbar con mensaje amigable + posibilidad de reintentar |

**HTTP errors:**

- *"Uso `Response<T>` de Retrofit para capturar códigos HTTP. Si la API responde con 404 o 500, `extractBody()` tira un `IOException` con el código y el mensaje. El `runCatching` en el Repository lo transforma en `Result.failure`."*
- *"En debug, el interceptor de OkHttp loguea bodies completos. En release, solo loguea headers para no exponer datos."*
- *"LeakCanary detecta memory leaks automáticamente en debug builds."*

### Timber vs android.util.Log

| Aspecto | `android.util.Log` | Timber |
|---|---|---|
| Tag automático | No, hay que definirlo manualmente | Sí, lo extrae del nombre de la clase automáticamente |
| Logs en release | Quedan si no los borrás manualmente | Solo se planta `DebugTree` en debug, release limpio |
| Excepciones | `Log.e(TAG, msg, tr)` el tag va primero | `Timber.e(tr, msg)` la excepción va primero, más natural |
| Formato | Solo concatenación de strings | Formato type-safe con `%s`, `%d`, etc. |
| Extensibilidad | No tiene | Podés crear Trees custom para Crashlytics, archivo, red |
| Crashlytics nativo | `FirebaseCrashlytics.log()` aparte | `Timber.plant(CrashlyticsTree())` en 1 línea |
| Autor | SDK de Android | Jake Wharton (Square, Dagger, Retrofit, OkHttp) |
| Adopción | SDK estándar | De facto en apps Android profesionales |

- *"Timber es un wrapper minimalista sobre `android.util.Log` resuelve problemas concretos: el tag automático elimina código repetitivo, y al plantarlo solo en debug te asegurás de no loguear datos en producción."*
- *"`Timber.e(throwable, format)` acepta la excepción como primer parámetro, lo que hace más difícil olvidar loguearla. Con `Log.e(TAG, msg, tr)` es fácil pasar el `Throwable` en otra posición y perder la traza."*
- *"La extensibilidad de Timber permite agregar árboles custom sin cambiar el código de logueo. Si más adelante quisiera integrar Firebase Crashlytics, alcanza con agregar `Timber.plant(CrashlyticsTree())` en el Application. Los `Timber.e()` existentes ya funcionan."*
- *"En este proyecto, reemplacé 3 `Log.e(TAG, ...)` en el Repository por `Timber.e(...)`. El tag se infiere solo, y la integración con el `isDebug` flag de Hilt garantiza que no haya logging en release."*

---

## 5. Testing

```
tests/
├── TestArticleData.kt          # Constantes de prueba compartidas
├── TestJson.kt                 # Fixtures JSON para MockWebServer
├── rules/
│   ├── MainDispatcherRule.kt    # Rule para coroutines
│   └── MockWebServerRule.kt    # Rule para servidor HTTP local
├── data/
│   ├── ApiServiceTest.kt       # 7 tests de integración HTTP
│   └── ArticlesRepositoryTest.kt # 7 tests del pipeline completo
├── ui/articles/list/
│   └── ArticlesListViewModelTest.kt  # 8 tests con MockK
└── ui/articles/detail/
    └── ArticleDetailViewModelTest.kt # 4 tests con MockK
```

**Lo que cubren los tests:**

- Success y failure de cada operación del ViewModel
- Paginación (append de artículos)
- Búsqueda (replace de resultados)
- Limpieza de estados (clearSearch, clearError)
- Casos borde (SavedStateHandle sin articleId)

**Tests unitarios vs de integración:**

| Tipo | Herramienta | Qué verifica |
|---|---|---|
| Unitarios (ViewModel) | MockK | Comportamiento del ViewModel con mock del repository |
| Integración HTTP | MockWebServer + Retrofit | Que el ApiService parsea bien el JSON, que el repository maneja HTTP codes |

**Por qué MockWebServer:**

- *"Mockea el servidor HTTP real. Retrofit le pega como si fuera la API de Space Flight News. Esto verifica que: 1) el endpoint está bien definido, 2) el JSON se serializa/deserializa correctamente, 3) los códigos HTTP de error se traducen a `Result.failure`."*
- *"Los tests de `ArticlesRepositoryTest` prueban `extractBody()` con HTTP 200, 404, 500 y JSON malformado. Cualquier cambio en el contrato de la API rompe el test, no en producción."*

**Por qué MockK y no Mockito:**

- *"MockK tiene soporte nativo para coroutines (`coEvery`, `coVerify`) y funciones `suspend`. Con Mockito necesitarías `mockito-kotlin` y extensiones adicionales."*

---

## 6. Clean Architecture + Repository Pattern (profundización)

### 6.1 ¿Qué es el Repository Pattern?

Es un patrón que **abstrae la fuente de datos detrás de una interfaz.** El que consume los datos (ViewModel, Use Case) no sabe ni le importa de dónde vienen: pueden venir de una API REST, una base de datos Room, un archivo local, o un mock de prueba.

```
ViewModel/UseCase → ArticlesRepository (interfaz) → DefaultArticlesRepository
                                                       ├── RemoteDataSource (API)
                                                       └── LocalDataSource (Room)
```

**Ventajas:**

| Beneficio | Explicación |
|---|---|
| Testeabilidad | En tests inyectás un mock del repositorio sin tocar la red |
| Swappable | Cambiás la fuente de datos sin tocar ViewModels ni UI |
| Single Source of Truth | El repositorio decide qué datos retornar y de dónde |
| Separación | La UI no sabe de Retrofit ni Room |

**En tu proyecto actual:**

```kotlin
interface ArticlesRepository {
    suspend fun getArticles(limit: Int, offset: Int): Result<List<Article>>
    suspend fun searchArticles(query: String, limit: Int): Result<List<Article>>
    suspend fun getArticle(id: Int): Result<Article>
}
```

El ViewModel solo conoce esta interfaz. En producción Hilt inyecta `DefaultArticlesRepository(apiService)`. En tests, MockK mockea la interfaz. El ViewModel es el mismo en ambos casos.

---

### 6.2 Las tres capas de Clean Architecture

Clean Architecture organiza el código en capas concéntricas donde **las capas internas no conocen a las externas.**

```
┌──────────────────────────────────────────────┐
│  presentation (UI, ViewModels, navegación)   │
│  ┌────────────────────────────────────────┐  │
│  │  domain (entidades, interfaces, casos) │  │
│  │  ┌──────────────────────────────────┐  │  │
│  │  │  data (APIs, DB, implementaciones)│  │  │
│  │  └──────────────────────────────────┘  │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘

       data → domain ← presentation
       (nadie depende de domain, domain depende de nadie)
```

---

#### 6.2.1 Capa `data` — "Cómo obtengo y guardo los datos"

**Contiene:** API services, DAOs, DataSources, implementaciones de repositorios, DTOs, entities de Room, mappers.

**No contiene:** Lógica de negocio, lógica de UI, ViewModels.

**Lo que NUNCA hace:** Decidir qué datos mostrar. Solo los obtiene y los devuelve.

**Ejemplo concreto si el proyecto tuviera Room:**

```kotlin
// data/datasource/remote/api/ApiService.kt
interface ApiService {
    @GET("articles/")
    suspend fun getArticles(
        @Query("limit") limit: Int, @Query("offset") offset: Int
    ): Response<ArticleResponse>
}

// data/datasource/remote/RemoteDataSource.kt
class RemoteDataSource @Inject constructor(private val api: ApiService) {
    suspend fun fetchArticles(limit: Int, offset: Int) =
        api.getArticles(limit, offset)
}

// data/datasource/remote/models/ArticleDto.kt
@Serializable
data class ArticleDto(
    val id: Int,
    val title: String,
    @SerialName("image_url") val imageUrl: String?,
    // ...
)

// data/datasource/local/dao/ArticlesDao.kt
@Dao
interface ArticlesDao {
    @Query("SELECT * FROM articles ORDER BY published_at DESC")
    fun getAll(): PagingSource<Int, ArticleEntity>

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles")
    suspend fun deleteAll()
}

// data/datasource/local/entities/ArticleEntity.kt
@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val imageUrl: String?,
    // ...
)

// data/datasource/local/LocalDataSource.kt
class LocalDataSource @Inject constructor(private val dao: ArticlesDao) {
    fun getArticlesPaged() = dao.getAll()
    suspend fun cacheArticles(articles: List<ArticleEntity>) = dao.insertAll(articles)
    suspend fun clearCache() = dao.deleteAll()
}

// data/Mappers.kt — convierte entre capas
fun ArticleDto.toEntity() = ArticleEntity(
    id = id, title = title, imageUrl = imageUrl
)

fun ArticleEntity.toDomain() = Article(
    id = id, title = title, imageUrl = imageUrl
)

// data/repository/ArticlesRepositoryImpl.kt
class ArticlesRepositoryImpl @Inject constructor(
    private val remote: RemoteDataSource,
    private val local: LocalDataSource,
) : ArticlesRepository {

    override fun getArticles(limit: Int, offset: Int): Flow<PagingData<Article>> =
        Pager(PagingConfig(pageSize = 20)) { local.getArticlesPaged() }
            .flow
            .map { it.map { entity -> entity.toDomain() } }

    override suspend fun refreshArticles() {
        val response = remote.fetchArticles(20, 0)
        if (response.isSuccessful) {
            local.clearCache()
            local.cacheArticles(response.body()!!.results.map { it.toEntity() })
        }
    }
}
```

**Puntos clave para defender:**

- *"Los DataSources (`RemoteDataSource`, `LocalDataSource`) son wrappers finos sobre APIs y DAOs. No tienen lógica, solo exponen los datos."*
- *"Los mappers (`toEntity()`, `toDomain()`) mantienen los modelos de cada capa separados. Un cambio en el JSON de la API no rompe la UI."*
- *"El repositorio implementado (`ArticlesRepositoryImpl`) orquesta ambos data sources. Decide cuándo usar caché y cuándo pegarle a la red. Esto es lo que hace que la app sea offline-first si se necesita."*

---

#### 6.2.2 Capa `domain` — "Qué reglas de negocio aplican"

**Contiene:** Entidades de dominio, interfaces de repositorio, use cases.

**No contiene:** Nada de Android (`Context`, `ViewModel`), nada de frameworks (`@GET`, `@Entity`).

**Es la capa más pura y la más importante.** Si migrás de Android a KMM o a un backend, esta capa se reutiliza.

**Ejemplo concreto:**

```kotlin
// domain/models/Article.kt
data class Article(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val summary: String,
    val publishedAt: String?,
    val newsSite: String?,
)

// domain/repository/ArticlesRepository.kt (interfaz, NO implementación)
interface ArticlesRepository {
    fun getArticles(): Flow<PagingData<Article>>
    suspend fun searchArticles(query: String): Result<List<Article>>
    suspend fun getArticle(id: Int): Result<Article>
    suspend fun refreshArticles()
}

// domain/usecases/GetArticlesUseCase.kt
class GetArticlesUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    operator fun invoke(): Flow<PagingData<Article>> = repository.getArticles()
}

// domain/usecases/SearchArticlesUseCase.kt
class SearchArticlesUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    operator fun invoke(query: String): Result<List<Article>> {
        // Acá iría la lógica de negocio real:
        // - Validar que el query tenga al menos 3 caracteres
        // - Sanitizar el input (trim, lowercase)
        // - Llamar al repositorio
        // - Si falla, intentar con caché local
        // - Transformar/ordenar resultados
        return repository.searchArticles(query.trim().lowercase())
    }
}
```

**El `operator fun invoke()`:** Es una convención de Kotlin que permite llamar al use case como si fuera una función:

```kotlin
// Sin operator invoke
val useCase = SearchArticlesUseCase(repository)
useCase.execute("nasa")

// Con operator invoke — más idiomático
val useCase = SearchArticlesUseCase(repository)
useCase("nasa")
```

**¿Cuándo agregan valor real los use cases?**

| Escenario | Sin Use Case | Con Use Case |
|---|---|---|
| Solo delegar al repo | `repo.searchArticles(q)` en el VM | Pasamanos, no suma |
| Validar input + delegar | Validación en el VM o repo | Lógica encapsulada en el use case |
| Combinar 2+ repositorios | Código duplicado en varios VMs | Un solo use case reutilizado |
| Transformar datos antes de retornar | Transform en el VM o mapper | Use case aplica la transformación |

---

#### 6.2.3 Capa `presentation` — "Qué ve y qué hace el usuario"

**Contiene:** ViewModels, UiState, Composable screens, Navigation.

**No contiene:** Lógica de negocio, acceso directo a APIs o bases de datos.

**Ejemplo concreto si hubiera use cases:**

```kotlin
// presentation/viewmodels/ArticlesViewModel.kt
@HiltViewModel
class ArticlesViewModel @Inject constructor(
    private val getArticles: GetArticlesUseCase,
    private val searchArticles: SearchArticlesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticlesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch {
            getArticles().collect { pagingData ->
                _uiState.update { it.copy(articles = pagingData) }
            }
        }
    }

    fun onSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            searchArticles(query)
                .onSuccess { _uiState.update { it.copy(results = it, isLoading = false) } }
                .onFailure { _uiState.update { it.copy(error = it.message, isLoading = false) } }
        }
    }
}
```

```kotlin
// presentation/ui/ArticlesScreen.kt
@Composable
fun ArticlesScreen(viewModel: ArticlesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        state.isLoading -> LoadingIndicator()
        state.error != null -> ErrorSnackbar(state.error!!)
        else -> ArticleList(state.articles, onArticleClick = { ... })
    }
}
```

---

### 6.3 La regla de dependencia (lo más importante)

```
  data ──conoce──→ domain ←──conoce── presentation
   ↑                   ↑                   ↑
   implementa      es independiente       consume
   interfaces      de las otras dos       interfaces
```

- **domain** no tiene imports de Android, Retrofit, Room, ni Compose. Es Kotlin puro.
- **data** implementa las interfaces que define domain. Conoce a domain.
- **presentation** consume las interfaces de domain. Conoce a domain.

El pegamento es **Hilt**, que en los módulos de `di/` le dice a data que implementación concreta usar para cada interfaz:

```kotlin
// di/RepositoryModule.kt
@Binds abstract fun bindArticlesRepository(impl: ArticlesRepositoryImpl): ArticlesRepository
```

Y en los módulos de use cases:

```kotlin
// di/UseCasesModule.kt
@Provides fun provideSearchArticlesUseCase(repo: ArticlesRepository) = SearchArticlesUseCase(repo)
```

---

### 6.4 ¿Por qué en este proyecto no hay capa `domain` con use cases?

1. **2 pantallas, 1 fuente de datos:** Lista y detalle. Una sola API REST. No hay Room, no hay SharedPreferences, no hay múltiples fuentes.

2. **Los use cases serían pasamanos vacíos:**
   ```kotlin
   class GetArticlesUseCase(repo: ArticlesRepository) {
       operator fun invoke() = repo.getArticles() // eso es todo
   }
   ```
   Esto no agrega valor. Crea archivos, imports, y boilerplate sin lógica real.

3. **El Repository Pattern es suficiente.** La interfaz `ArticlesRepository` ya desacopla el ViewModel de la fuente de datos. Con eso alcanza para testeabilidad.

4. **YAGNI (You Ain't Gonna Need It).** Si el proyecto creciera (offline-first, sincronización, múltiples pantallas que comparten lógica), ahí se justificaría refactorizar.

---

### 6.5 ¿Cuándo SÍ se justifica Clean Architecture completa?

| Señal | Acción |
|---|---|
| Más de una fuente de datos (API + Room) | Agregar `LocalDataSource` + mappers |
| Lógica de negocio en el ViewModel | Extraer a use cases en `domain/` |
| Código repetido entre ViewModels | Unificar en use cases |
| Múltiples módulos o features | Separar por feature con su propia capa domain |
| Proyecto con +5 pantallas | Clean Architecture desde el día 1 |

---

### 6.6 Si tuvieras que agregar Room y offline-first, ¿cómo lo harías?

1. Creás `ArticleEntity` con anotaciones de Room en `data/datasource/local/entities/`
2. Creás `ArticlesDao` en `data/datasource/local/dao/`
3. Creás `AppDatabase` (Room)
4. Creás `LocalDataSource` que wrappea el DAO
5. Creás mappers `ArticleDto.toEntity()`, `ArticleEntity.toDomain()`
6. Modificás el repositorio para que:
   - Primero devuelva datos de Room (respuesta inmediata)
   - Después pegue a la API (refresh en background)
   - Si la API falla, ya hay datos en Room (offline)
7. Agregás un `RefreshArticlesUseCase` en `domain/`
8. El ViewModel no cambia. Sigue consumiendo la misma interfaz `ArticlesRepository`.

**Para defender:** *"Agregar offline-first sin tocar el ViewModel ni la UI es exactamente el propósito de esta arquitectura. Las capas internas no se enteran de los cambios en las externas."*

---

## 7. Decisiones técnicas adicionales

### Coil vs Glide
- *"Coil 3 tiene soporte nativo para Compose, Kotlin coroutines y OkHttp. Glide requiere adapters adicionales para funcionar con Compose."*

### kotlinx.serialization vs Gson
- *"Es nativo de Kotlin, no usa reflection (más rápido, más seguro), y se integra con los type-safe routes de Navigation Compose."*

### StateFlow vs LiveData
- *"StateFlow es reactivo, tiene soporte nativo para coroutines, y `collectAsStateWithLifecycle()` respeta el lifecycle de Compose. LiveData está diseñado para Views/Fragments, no para Compose."*

### Paging
- *"No implementé Paging3 porque el endpoint de Space Flight News devuelve listas paginadas con `offset`/`limit`. Para 2 pantallas, la paginación manual con `LazyColumn` + `derivedStateOf` es suficiente."*

### buildSrc vs Version Catalog (TOML)

| Aspecto | TOML (lo que uso) | buildSrc |
|---|---|---|
| Sintaxis | Declarativo (`module = "..."`) | Kotlin (`implementation(Dependencies.retrofit)`) |
| IDE support | Autocompletado parcial | Autocompletado, go-to-definition, refactoring |
| Error detection | En runtime (al resolver) | En compilación |
| Velocidad de sync | Instantáneo | buildSrc compila primero (+3s por sync) |
| Custom logic | No soporta | Funciones helper, extensiones, lo que quieras |
| Recomendación oficial | **Sí**, Google recomienda version catalogs desde Gradle 7.0 | Era el estándar antes de version catalogs |
| Escalabilidad | Bien hasta ~30 deps en 1 módulo | Mejor con +50 deps y múltiples módulos |

**Para defender:** *"Usé TOML version catalog porque es la recomendación oficial de Gradle desde la 7.0. buildSrc era el estándar antes de que existieran los version catalogs y tiene ventajas como type-safety y custom logic, pero para un proyecto de 1 módulo con ~20 dependencias, TOML es más simple, más rápido y es lo que Google recomienda hoy."*

### CoroutineDispatcher DI (injectable dispatchers)

```kotlin
@Module @InstallIn(SingletonComponent::class)
object DispatcherModule {
    @IoDispatcher @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @DefaultDispatcher @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
// uso: class MiRepo @Inject constructor(@IoDispatcher val io: CoroutineDispatcher)
```

**Para defender:** *"Este patrón permite inyectar dispatchers vía Hilt en vez de hardcodear `Dispatchers.IO` en el repository. En tests de integración, puedo reemplazar el módulo de Hilt para proveer `StandardTestDispatcher` en vez de `Dispatchers.IO`, eliminando la necesidad de `Dispatchers.setMain()`."*

*"En este proyecto no lo apliqué a producción porque:*
1. *Retrofit maneja su propio threading sobre OkHttp dispatcher. Agregar `withContext(IO)` es redundante.*
2. *Los ViewModels usan `viewModelScope` que ya despacha en Main.*
3. *No hay Room ni operaciones CPU-bound en el repository.*

*Pero el módulo está declarado y listo para usar si el proyecto creciera. Es más: si mañana agrego Room, el `LocalDataSource` recibe `@IoDispatcher` sin tocar el ViewModel."*

**Valor en entrevista:** Mostrar este patrón revela que:
- Entendés que `Dispatchers.IO` acoplado al código es una dependencia oculta
- Sabés que un repository testeable necesita que el dispatcher sea inyectable
- Conocés `@Qualifier` de Dagger/Hilt y cómo crear anotaciones custom

### SplashScreen API vs Lottie para animaciones de carga

**No compiten, se complementan.** Resuelven problemas distintos.

| Aspecto | SplashScreen API | Lottie |
|---|---|---|
| Quién | Google (parte del OS desde Android 12) | Airbnb (librería externa) |
| Cuándo | Antes de que tu Activity exista (cold start) | Dentro de la app, en cualquier momento |
| Qué muestra | Icono de la app + color de fondo (solo eso) | Animaciones After Effects complejas |
| Personalización | Mínima (color de fondo, icono) | Total (cualquier animación JSON) |
| Animación | Fade-out del icono al entrar a la app | Loops, morphing, interactivas |

**Para defender:** *"Usé la SplashScreen API de Android porque es la solución oficial del sistema operativo. Mientras la app carga, el sistema ya muestra el icono en el color que definí. No hay stutter ni flash blanco. Lottie no podría hacer esto porque la app ni siquiera existe cuando la SplashScreen aparece."*

*"Lottie lo usé para reemplazar el `CircularProgressIndicator` del loading de artículos y detalle. Una animación temática de Space Flight News (cohete/espacio) se ve mucho más profesional que un spinner gris. El evaluador lo nota al toque."*

**¿Por qué no Lottie en la splash?** *"Lottie aparece cuando la app ya inició. Si intentara usarlo como splash, habría un delay entre que el sistema muestra el icono y Lottie se renderiza. La SplashScreen API resuelve exactamente ese problema."*

**¿Por qué no la SplashScreen API para reemplazar el loading?** *"La SplashScreen API solo se muestra una vez al abrir la app. Si el usuario navega a detalle, no puede usarse. Ahí entra Lottie."*

**La combinación ideal:**
1. Sistema: SplashScreen API → icono desde que tocás el app hasta que Compose renderiza
2. App: Lottie → animaciones temáticas mientras cargan datos
3. No hay 3.

---

## 8. Si pudieras volver a empezar, ¿qué harías diferente?

Buena pregunta para el final. Respuesta honesta:

1. *"No habría usado navigation3. Perdí tiempo debugueando problemas de una librería experimental."*
2. *"Agregaría tests de integración con MockWebServer para la capa HTTP. Los tests unitarios de ViewModels están bien, pero un test que verifique que el parseo de la API funciona da más seguridad."*
3. *"En cuanto a buildSrc vs TOML, me quedo con TOML: es el approach moderno y recomendado. buildSrc lo conozco pero no suma nada acá (ver sección 7.5)."*
