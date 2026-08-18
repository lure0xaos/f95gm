package f95gm.client.data.json

import f95gm.domain.defaults.F95Defaults
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.string(key: String, fallback: String = F95Defaults.EMPTY): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: fallback
