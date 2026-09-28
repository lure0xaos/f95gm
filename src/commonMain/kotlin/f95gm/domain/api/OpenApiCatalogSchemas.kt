package f95gm.domain.api

import kotlinx.serialization.json.JsonObject

internal fun catalogSchemaShape(schema: ApiSchema): JsonObject? = when (schema) {
    ApiSchema.CATALOG_OPTIONS -> propertiesSchema("tags" to "string-map", "prefixes" to "any-object-or-array")
    else -> null
}
