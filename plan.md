# Plan: Space Flight News App

## API
- Base URL: `https://api.spaceflightnewsapi.net/v4`
- Endpoints:
  - `GET /articles/?limit=20&offset=N` → listado paginado
  - `GET /articles/?search=<query>` → búsqueda
  - `GET /articles/<id>/` → detalle
- Modelo Article: `id`, `title`, `authors`, `url`, `image_url`, `news_site`, `summary`, `published_at`

## Dependencias nuevas
- Retrofit + OkHttp → HTTP
- kotlinx.serialization converter → serialización JSON
- Coil → carga de imágenes

## Arquitectura
- MVVM + Repository pattern
- UI State: sealed interface (Loading, Success, Error)
- Result wrapper en Repository

## Pantallas
| Pantalla | Vista | ViewModel |
|---|---|---|
| ArticlesList | SearchBar + LazyColumn | ArticlesListViewModel |
| ArticleDetail | Imagen, título, autores, fecha, summary | ArticleDetailViewModel |

## Manejo de errores
- Developer: try/catch + Log.e() + Result.failure
- Usuario: Snackbar + Retry

## Rotación
- ViewModels sobreviven (by design)
- searchQuery en SavedStateHandle

## Tests
- Unit tests para ViewModels mockeando Repository

## Sin permisos adicionales (INTERNET incluido por defecto)
