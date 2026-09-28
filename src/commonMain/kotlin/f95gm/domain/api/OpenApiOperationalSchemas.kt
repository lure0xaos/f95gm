package f95gm.domain.api

import kotlinx.serialization.json.JsonObject

internal fun operationalSchemaShape(schema: ApiSchema): JsonObject? = when (schema) {
    ApiSchema.EMPTY_REQUEST -> objectSchema()
    ApiSchema.REQUEST_METRICS -> propertiesSchema(
        "requests" to "integer", "rejected" to "integer", "totalDurationMillis" to "integer",
        "retries" to "integer", "evictions" to "integer", "circuitOpenings" to "integer", "endpointRequests" to "number-map",
        "upstream" to "object", "cacheShutdownRejections" to "integer", "cacheWaiterCancellations" to "integer",
        "cacheActiveLoadInvalidations" to "integer", "cacheStaleLoadCompletions" to "integer",
        "countersConsistent" to "boolean"
    )
    else -> null
}
