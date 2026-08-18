package f95gm.server.routes.marks.live

import f95gm.domain.defaults.F95Defaults
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.string(key: String, fallback: String = F95Defaults.EMPTY): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: fallback
