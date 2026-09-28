package f95gm.client.test

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.assertNotNull

internal fun JsonObject.required(path: String): JsonElement {
    var current: JsonElement = this
    path.split('.').forEach { segment ->
        current = assertNotNull(current.jsonObject[segment], "Missing JSON field '$path' at '$segment'")
    }
    return current
}

internal fun JsonObject.requiredString(path: String): String = required(path).jsonPrimitive.content
