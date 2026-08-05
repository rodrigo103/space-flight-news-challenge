package com.example.myandroidapp.analytics

import com.example.myandroidapp.util.AppLogger

/**
 * Reemplaza a `TimberAnalyticsHelper`, que era Android-only. Loguea vía [AppLogger], que resuelve
 * a Timber en Android y `NSLog` en iOS, así que el mismo helper sirve en las dos plataformas.
 *
 * Mantiene el tag `Analytics` para no romper el filtrado de Logcat existente.
 */
class LoggingAnalyticsHelper : AnalyticsHelper {

    override fun logEvent(name: String, params: Map<String, String>) {
        AppLogger.d("Event: $name | params: $params", TAG)
    }

    override fun logError(throwable: Throwable, context: String) {
        AppLogger.e(throwable, "Error: $context", TAG)
    }

    override fun logScreenView(screenName: String) {
        AppLogger.d("Screen View: $screenName", TAG)
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
