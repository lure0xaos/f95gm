package f95gm.domain.api

import kotlinx.serialization.Serializable

@Serializable
data class F95OpenApiInfo(
    val title: String = "F95 Game Manager API",
    val version: String = "1.0.0"
)
