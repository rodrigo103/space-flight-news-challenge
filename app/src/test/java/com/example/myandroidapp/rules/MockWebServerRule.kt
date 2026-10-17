package com.example.myandroidapp.rules

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Helper de test (JUnit [TestRule], no es un doble en sí): levanta un [MockWebServer] antes de cada
 * test y lo apaga en el `finally`.
 *
 * Es la pieza que monta el STUB HTTP: `enqueueJson(code, body)` encola respuestas enlatadas y
 * `baseUrl()` dirige Retrofit hacia el server. El `server` expuesto también habilita el uso SPY
 * (`takeRequest()`/`requestCount`) en los tests que eligen assertear la request.
 */
class MockWebServerRule : TestRule {

    val server = MockWebServer()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                server.start()
                try {
                    base.evaluate()
                } finally {
                    server.shutdown()
                }
            }
        }
    }

    fun enqueueJson(code: Int, body: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(body)
                .addHeader("Content-Type", "application/json"),
        )
    }

    fun baseUrl() = server.url("/").toString()
}
