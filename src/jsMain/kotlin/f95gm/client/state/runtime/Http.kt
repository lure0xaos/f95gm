package f95gm.client.state.runtime

import f95gm.client.state.config.ClientConstants
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*

internal val http = HttpClient(Js) {
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = ClientConstants.REQUEST_TIMEOUT_MILLIS
    }
}
