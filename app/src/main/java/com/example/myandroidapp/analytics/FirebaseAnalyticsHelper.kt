package com.example.myandroidapp.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsHelper @Inject constructor(
    @ApplicationContext context: Context,
) : AnalyticsHelper {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun logEvent(name: String, params: Map<String, String>) {
        firebaseAnalytics.logEvent(name) {
            params.forEach { (key, value) -> param(key, value) }
        }
    }

    override fun logError(throwable: Throwable, context: String) {
        firebaseAnalytics.logEvent("app_error") {
            param("context", context)
            param("error", throwable.message ?: "Unknown")
        }
    }

    override fun logScreenView(screenName: String) {
        // Firebase auto-tracks screen views via Compose / Activity lifecycle
    }
}
