package f95gm.client.state.runtime

import f95gm.client.state.config.ClientConstants
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*

internal fun createHttpClient(): HttpClient = HttpClient(Js) {
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = ClientConstants.REQUEST_TIMEOUT_MILLIS
    }
}

/** Legacy request helpers still use this shared client; runtimes should inject an owned client. */
