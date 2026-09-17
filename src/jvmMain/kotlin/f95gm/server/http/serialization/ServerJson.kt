package f95gm.server.http.serialization

import kotlinx.serialization.json.Json

/** Shared HTTP serialization policy; runtime services do not own wire encoding. */
internal val upstreamJson: Json = Json {
    ignoreUnknownKeys = true
}
