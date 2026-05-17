package com.example.myandroidapp.analytics

interface AnalyticsHelper {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun logError(throwable: Throwable, context: String = "")
    fun logScreenView(screenName: String)
}
