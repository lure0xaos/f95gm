package f95gm.server.app

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.fail

internal fun JsonObject.requiredOperation(method: String): JsonObject = requiredObject(method.lowercase())

internal fun JsonObject.requiredObject(name: String): JsonObject = get(name)?.jsonObject
    ?: fail("Expected JSON object property '$name'")

internal fun JsonObject.requiredArray(name: String): JsonArray = get(name)?.jsonArray
    ?: fail("Expected JSON array property '$name'")

internal fun JsonObject.requiredPrimitive(name: String): JsonPrimitive = get(name)?.jsonPrimitive
    ?: fail("Expected JSON primitive property '$name'")
