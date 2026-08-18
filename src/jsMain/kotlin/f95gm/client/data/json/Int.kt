package f95gm.client.data.json

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.int(key: String, fallback: Int): Int =
    this[key]?.jsonPrimitive?.intOrNull ?: fallback
