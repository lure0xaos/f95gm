package f95gm.domain.api

import kotlinx.serialization.json.JsonObject

/** HTTP method -> OpenAPI operation document. */
typealias F95OpenApiOperations = Map<String, JsonObject>
