package f95gm.domain.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

internal fun objectSchema(additionalProperties: Boolean = false): JsonObject = buildJsonObject {
    put("type", JsonPrimitive("object"))
    if (additionalProperties) put("additionalProperties", JsonPrimitive(true))
}

internal fun arraySchema(itemRef: String): JsonObject = buildJsonObject {
    put("type", JsonPrimitive("array"))
    put("items", if (itemRef.startsWith("primitive:")) {
        buildJsonObject { put("type", JsonPrimitive(itemRef.removePrefix("primitive:"))) }
    } else buildJsonObject { put("\$ref", JsonPrimitive("#/components/schemas/$itemRef")) })
}

internal fun propertiesSchema(vararg properties: Pair<String, String>): JsonObject = buildJsonObject {
    put("type", JsonPrimitive("object"))
    put("properties", buildJsonObject {
        properties.forEach { (name, type) ->
            put(name, when {
                type.startsWith("#") -> buildJsonObject { put("\$ref", JsonPrimitive(type)) }
                type.startsWith("array:") -> arraySchema(type.removePrefix("array:"))
                type == "string-map" -> buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("additionalProperties", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
                type == "number-map" -> buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("additionalProperties", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
                type == "any-object-or-array" -> buildJsonObject {
                    put("anyOf", buildJsonArray {
                        add(buildJsonObject { put("type", JsonPrimitive("object")) })
                        add(buildJsonObject { put("type", JsonPrimitive("array")) })
                    })
                }
                else -> buildJsonObject { put("type", JsonPrimitive(type)) }
            })
        }
    })
}

internal fun JsonObject.withRequired(vararg names: String): JsonObject = buildJsonObject {
    this@withRequired.forEach { (key, value) -> put(key, value) }
    put("required", buildJsonArray { names.forEach { add(JsonPrimitive(it)) } })
}
