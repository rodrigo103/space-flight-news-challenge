package com.example.myandroidapp.analytics

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimberAnalyticsHelper @Inject constructor() : AnalyticsHelper {

    override fun logEvent(name: String, params: Map<String, String>) {
        Timber.tag("Analytics").d("Event: %s | params: %s", name, params)
    }

    override fun logError(throwable: Throwable, context: String) {
        Timber.tag("Analytics").e(throwable, "Error: %s", context)
    }

    override fun logScreenView(screenName: String) {
        Timber.tag("Analytics").d("Screen View: %s", screenName)
    }
}
