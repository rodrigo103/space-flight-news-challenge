# Retrofit Setup

> **Last verified:** 2026-05-17 | **Verified by:** [source]

## Config

- Base URL: `https://api.spaceflightnewsapi.net/v4`
- Converter: `kotlinx.serialization` (vía `Json { ignoreUnknownKeys = true }`)
- `OkHttpClient` con logging interceptor en debug
- `HttpErrorCallAdapter.Factory` — custom factory que wrappea errores HTTP [source]

## ApiService

```kotlin
interface ApiService {
    @GET("articles/")
    suspend fun getArticles(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
    ): Response<ArticleResponse>

    @GET("articles/{id}/")
    suspend fun getArticle(@Path("id") id: Int): Response<Article>
}
```

## HTTP Error handling

`HttpErrorCallAdapter` intercepta respuestas con código HTTP ≠ 2xx y crea `ApiException` con el código y mensaje del error. Esto evita tener que checkear `response.isSuccessful` manualmente en cada llamada.

## Dependencias

- `com.squareup.retrofit2:retrofit`
- `com.squareup.retrofit2:converter-kotlinx-serialization`
- `com.squareup.okhttp3:okhttp`
- `com.squareup.okhttp3:logging-interceptor`

## Module DI

`NetworkModule` (Singleton):
1. Provee `OkHttpClient` con logging interceptor
2. Provee `Retrofit` instance con base URL + kotlinx serialization converter
3. Provee `ApiService` implementado por Retrofit