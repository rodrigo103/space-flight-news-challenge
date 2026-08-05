package com.example.myandroidapp.util

/**
 * Logger mínimo para `commonMain`. Timber es JVM-only, así que cada plataforma provee su `actual`:
 * Timber en Android, `NSLog` en iOS.
 */
expect object AppLogger {
    fun d(message: String)
    fun e(throwable: Throwable?, message: String)
}
