# Data Layer

> **Last verified:** 2026-05-17 | **Verified by:** [source]

Data layer del proyecto. Sigue patrón Repository con dos fuentes: API remota (Retrofit) y base local (Room).

## Network layer

```
ApiService (Retrofit)
    ├── GET /articles/?limit=&offset=&search=
    └── GET /articles/{id}/
```

- Base URL: `https://api.spaceflightnewsapi.net/v4`
- `OkHttpClient` con logging interceptor en debug
- `kotlinx.serialization` converter (no Gson)
- `HttpErrorCallAdapter` — custom CallAdapter.Factory que wrappea errores HTTP en `ApiException` [source]
- `ResponseExt` — extensiones para parsear `Response<T>` a `Result<T>` con `ApiException` [source]

## Room layer

- `AppDatabase` con migrations (Room 2.7+)
- `ArticleEntity` — entidad Room para artículos (id, title, authors, url, image_url, news_site, summary, published_at)
- `ArticleDao` — DAO con consultas Paging:
  - `getAllPaging()` → `PagingSource<Int, ArticleEntity>`
  - `search(searchQuery)` → `PagingSource<Int, ArticleEntity>` con `LIKE` query

## Paging 3 + RemoteMediator

- `ArticleRemoteMediator` — sincroniza API → Room con paginación:
  1. Carga página desde API
  2. Inserta en Room
  3. Room via PagingSource alimenta la UI
  4. En scroll infinito, RemoteMediator pide siguiente página

## Repository pattern

`ArticlesRepository` orquesta las fuentes [source]:
- `getArticles()` → `Flow<PagingData<Article>>` (vía RemoteMediator)
- `searchArticles(query)` → `Flow<PagingData<Article>>` (búsqueda con RemoteMediator)
- `getArticleById(id)` → `Result<Article>` (primero Room, si falla API)

## DataStore Preferences

- `AppPreferences` — wrapper sobre `DataStore<Preferences>` [source]
- Guarda preferencias de usuario (tema, etc.)