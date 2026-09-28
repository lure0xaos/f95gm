package f95gm.domain.api

import kotlinx.serialization.json.JsonObject

internal fun markSchemaShape(schema: ApiSchema): JsonObject? = when (schema) {
    ApiSchema.MARKS -> arraySchema("MarkResponse")
    ApiSchema.MARK_RESPONSE -> propertiesSchema(
        "threadId" to "string", "trackingState" to "string", "title" to "string", "version" to "string",
        "developer" to "string", "cover" to "string", "prefixes" to "array:primitive:string", "date" to "string",
        "likes" to "string", "views" to "string", "rating" to "number", "isNew" to "boolean",
        "storedVersion" to "string", "latestVersion" to "string", "updateAvailable" to "boolean", "updatedAt" to "integer"
    )
    else -> null
}
