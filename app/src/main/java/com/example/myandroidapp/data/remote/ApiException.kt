package com.example.myandroidapp.data.remote

sealed class ApiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class BadRequest(message: String) : ApiException(message)
    class Unauthorized(message: String) : ApiException(message)
    class NotFound(message: String) : ApiException(message)
    class Conflict(message: String) : ApiException(message)
    class ServerError(val code: Int, message: String) : ApiException(message)
    class NetworkError(cause: Throwable) : ApiException("Network error", cause)
}
