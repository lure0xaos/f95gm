package f95gm.server.routes.marks.live

import kotlinx.serialization.json.JsonObject

internal fun JsonObject.jsonObject(key: String): JsonObject? = this[key] as? JsonObject
