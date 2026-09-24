package f95gm.server.http.request

import f95gm.server.state.runtime.upstreamJson

internal inline fun <reified T> decodeJsonOrNull(body: String): T? =
    runCatching { upstreamJson.decodeFromString<T>(body) }.getOrNull()
