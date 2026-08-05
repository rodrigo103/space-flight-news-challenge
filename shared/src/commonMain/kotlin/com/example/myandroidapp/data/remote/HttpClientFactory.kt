package com.example.myandroidapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val BASE_URL = "https://api.spaceflightnewsapi.net/v4"

private const val BAD_REQUEST = 400
private const val UNAUTHORIZED = 401
private const val NOT_FOUND = 404
private const val CONFLICT = 409
private const val SERVER_ERROR_MIN = 500
private const val SERVER_ERROR_MAX = 599
private const val REDIRECT_MIN = 300

/**
 * Configuración compartida por producción y tests. Los tests instalan la misma sobre `MockEngine`,
 * de forma que el mapeo de errores que se testea es exactamente el que corre en la app.
 */
fun HttpClientConfig<*>.configureApiClient(enableLogging: Boolean = false) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }

    if (enableLogging) {
        install(Logging) { level = LogLevel.BODY }
    }

    // Reemplaza a HttpErrorCallAdapterFactory: traduce status codes a ApiException tipadas
    // antes de que la respuesta llegue al repositorio.
    HttpResponseValidator {
        validateResponse { response ->
            val code = response.status.value
            if (code < REDIRECT_MIN) return@validateResponse

            val details = runCatching { response.bodyAsText() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: response.status.description
            val message = "HTTP $code: $details"

            throw when (code) {
                BAD_REQUEST -> ApiException.BadRequest(message)
                UNAUTHORIZED -> ApiException.Unauthorized(message)
                NOT_FOUND -> ApiException.NotFound(message)
                CONFLICT -> ApiException.Conflict(message)
                in SERVER_ERROR_MIN..SERVER_ERROR_MAX -> ApiException.ServerError(code, message)
                else -> ApiException.NetworkError(IllegalStateException(message))
            }
        }
    }
}

/** OkHttp en Android, Darwin en iOS. */
expect fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient

fun createApiHttpClient(enableLogging: Boolean = false): HttpClient =
    createPlatformHttpClient { configureApiClient(enableLogging) }

/**
 * Punto de entrada para los consumidores del módulo. Devuelve `ApiService` y no `HttpClient`
 * para que Ktor no aparezca en las firmas de `:app` ni del `iosApp`.
 */
fun createApiService(enableLogging: Boolean = false): ApiService =
    ApiService(createApiHttpClient(enableLogging))
