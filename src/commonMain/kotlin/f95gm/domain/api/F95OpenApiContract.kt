package f95gm.domain.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class F95OpenApiContract(
    val openapi: String = "3.0.3",
    val info: F95OpenApiInfo = F95OpenApiInfo(),
    val paths: F95OpenApiPaths,
    val components: JsonObject
)
