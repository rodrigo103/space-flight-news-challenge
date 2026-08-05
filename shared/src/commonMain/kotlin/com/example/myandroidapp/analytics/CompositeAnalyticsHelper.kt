package com.example.myandroidapp.analytics

class CompositeAnalyticsHelper(
    private val delegates: List<AnalyticsHelper>,
) : AnalyticsHelper {

    override fun logEvent(name: String, params: Map<String, String>) {
        delegates.forEach { it.logEvent(name, params) }
    }

    override fun logError(throwable: Throwable, context: String) {
        delegates.forEach { it.logError(throwable, context) }
    }

    override fun logScreenView(screenName: String) {
        delegates.forEach { it.logScreenView(screenName) }
    }
}
