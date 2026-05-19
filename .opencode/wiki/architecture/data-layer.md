---
tags:
  - wiki/architecture
---

# Data Layer

> **Last verified:** 2026-05-19 | **Verified by:** [source] — reorganized into `domain/` + `data/` layers; API files moved to `data/remote/`, mappers extracted to `data/mappers/`

Data layer del proyecto. Sigue patrón Repository con dos fuentes: API remota (Retrofit) y base local (Room). La interfaz del repositorio vive en `domain/repository/`, la implementación en `data/repository/`.

## Network layer (`data/remote/`)

```
ApiService (Retrofit)
    ├── GET /articles/?limit=&offset=&search=  → ArticleResponse
    └── GET /articles/{id}/                     → Article
```

- Base URL: `https://api.spaceflightnewsapi.net/v4` — API agregadora, no hostea contenido completo
- Ambos endpoints (`list` y `detail`) devuelven el **mismo schema** de artículo: `id`, `title`, `authors`, `url`, `image_url`, `news_site`, `summary`, `published_at`, `updated_at`, `featured`, `launches`, `events`. No existe campo `body`/`content`. [source]
- El endpoint de detalle se llama por corrección arquitectónica (fetch fresco, cache fallback), aunque los datos sean redundantes con el listado. Ver [[patterns/mvvm-repository]]. [analysis]
- `OkHttpClient` con logging interceptor en debug
- `kotlinx.serialization` converter (no Gson)
- `HttpErrorCallAdapterFactory` — `CallAdapter.Factory` registrado en `NetworkModule`. Intercepta `enqueue()` y convierte respuestas no-2xx en `ApiException` (BadRequest, Unauthorized, NotFound, Conflict, ServerError) o `IOException`. [source]
- APIs devuelven dominio directo (`ArticleResponse`, `Article`), no `Response<T>`. El error handling es automático via `CallAdapter`.
- `ResponseExt.kt` fue eliminado — ya no se necesita `extractBody()`.

## Room layer (`data/local/`)

- `AppDatabase` con migrations (Room 2.7+)
- `ArticleEntity` — entidad Room para artículos (id, title, authors, url, image_url, news_site, summary, published_at)
- `ArticleDao` — DAO con consultas Paging:
  - `getAllPaging()` → `PagingSource<Int, ArticleEntity>`
  - `search(searchQuery)` → `PagingSource<Int, ArticleEntity>` con `LIKE` query

## Mappers (`data/mappers/`)

- `ArticleMappers.kt` — Extension functions `ArticleEntity.toArticle()` y `Article.toEntity()`. Extraídas de `ArticleEntity.kt` para mantener la entidad Room libre de lógica de mapeo.

## Paging 3 + RemoteMediator

- `ArticleRemoteMediator` — sincroniza API → Room con paginación:
  1. Carga página desde API
  2. Inserta en Room
  3. Room via PagingSource alimenta la UI
  4. En scroll infinito, RemoteMediator pide siguiente página

## Repository pattern

Interfaz en `domain/repository/ArticlesRepository.kt`, implementación en `data/repository/DefaultArticlesRepository.kt`.

`DefaultArticlesRepository` orquesta las fuentes [source]:
- `getArticles(limit, offset)` → `Result<List<Article>>` (via `apiService.getArticles(...).results`)
- `searchArticles(query, limit)` → `Result<List<Article>>` (via `apiService.getArticles(...).results`)
- `getArticle(id)` → `Result<Article>` (via `apiService.getArticle(id)`)
- `getCachedArticle(id)` → `Article?` (via Room `getById()`)
- `getArticlesPaged(searchQuery?)` → `Flow<PagingData<Article>>` (via RemoteMediator + Room)
- Errores capturados con `runCatching { ... }.onFailure { Timber.e(...) }`

La interfaz `ArticlesRepository` es pura (solo tipos del dominio), permitiendo que los ViewModels y UseCases dependan de la abstracción sin conocer los detalles de implementación. [analysis]
