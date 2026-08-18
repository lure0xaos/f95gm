package f95gm.server.routes.marks.live

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.boolean(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull == true
