package f95gm.server.state.runtime

import kotlinx.serialization.json.Json

internal val upstreamJson = Json {
    ignoreUnknownKeys = true
}
