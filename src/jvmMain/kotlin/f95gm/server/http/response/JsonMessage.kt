package f95gm.server.http.response

import f95gm.server.model.http.JsonMessage
import f95gm.server.state.runtime.upstreamJson

internal fun jsonMessage(message: String): String = upstreamJson.encodeToString(JsonMessage(message))
