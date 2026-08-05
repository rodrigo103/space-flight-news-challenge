package com.example.myandroidapp.util

/**
 * Logger mínimo para `commonMain`. Timber es JVM-only, así que cada plataforma provee su `actual`:
 * Timber en Android, `NSLog` en iOS.
 *
 * Los valores por defecto van en el `expect` (Kotlin no los permite en los `actual`).
 */
expect object AppLogger {
    fun d(message: String, tag: String? = null)
    fun e(throwable: Throwable?, message: String, tag: String? = null)
}
