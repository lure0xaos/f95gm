package f95gm.client.data.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.booleanOrNull
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.json.jsonBooleanOrNull
import f95gm.domain.json.jsonDoubleOrNull
import f95gm.domain.json.jsonStringList
import f95gm.domain.json.jsonText

internal fun JsonObject.objectValue(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.arrayValue(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonObject.primitiveValue(key: String): JsonPrimitive? = this[key] as? JsonPrimitive
internal fun JsonObject.string(key: String, fallback: String = F95Defaults.EMPTY): String =
    primitiveValue(key)?.contentOrNull ?: fallback

internal fun JsonElement.stringValue(key: String): String =
    (this as? JsonObject)?.get(key)?.jsonText().orEmpty()

internal fun JsonElement.arrayStrings(): List<String> =
    if (this is JsonArray) jsonStringList() else emptyList()

internal fun JsonElement.objectArray(): List<JsonObject> =
    (this as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

internal fun JsonElement.doubleValue(): Double? = jsonDoubleOrNull()
internal fun JsonElement.booleanValue(): Boolean? = jsonBooleanOrNull()
