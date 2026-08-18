package f95gm.server.state.runtime

import io.ktor.client.*
import io.ktor.client.engine.cio.*

internal val client = HttpClient(CIO) {
    expectSuccess = false
    followRedirects = false
}
