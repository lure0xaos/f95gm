package f95gm.domain.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/** Declarative list of schemas published by the contract; shapes remain generated from typed schema keys. */
internal val publishedOpenApiSchemas: List<ApiSchema> get() = OpenApiSchemaCatalog.published


internal fun ApiSchema.requiredOpenApiFields(): List<String> = OpenApiSchemaCatalog.requiredFields(this)

internal fun JsonObject.withRegistryRequirements(schema: ApiSchema): JsonObject {
    val requiredFields = schema.requiredOpenApiFields()
    if (requiredFields.isEmpty()) return this
    if ("required" in keys) return this
    return buildJsonObject {
        forEach { (key, value) -> put(key, value) }
        put("required", buildJsonArray { requiredFields.forEach { field -> add(JsonPrimitive(field)) } })
    }
}

/** Compatibility facade; schema ownership and shape definitions live in the catalog. */
internal fun ApiSchema.openApiShape(): JsonObject = OpenApiSchemaCatalog.typedShape(this)
