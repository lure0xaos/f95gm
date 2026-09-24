package f95gm.server.state.runtime

import f95gm.server.config.Millis

internal data class HttpClientTimeoutPolicy(
    val requestMillis: Millis,
    val connectMillis: Millis,
    val socketMillis: Millis
)
