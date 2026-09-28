package f95gm.domain.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/** Shared tolerant conversion policy for irregular external JSON values. */
fun JsonElement.jsonText(fallback: String = ""): String =
    (this as? JsonPrimitive)?.contentOrNull ?: fallback

fun JsonElement.jsonStringList(): List<String> = when (this) {
    is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
    is JsonPrimitive -> contentOrNull?.takeIf(String::isNotBlank)?.let(::listOf).orEmpty()
    else -> emptyList()
}

fun JsonElement.jsonDoubleOrNull(): Double? = (this as? JsonPrimitive)?.doubleOrNull

fun JsonElement.jsonBooleanOrNull(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

fun JsonElement.jsonStringMap(): Map<String, String> =
    (this as? JsonObject).orEmpty().mapNotNull { (key, value) ->
        (value as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)?.let { key to it }
    }.toMap()
