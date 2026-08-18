package f95gm.server.model.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class LatestUpdatesConfig(
    val tags: JsonObject = JsonObject(emptyMap()),
    val prefixes: JsonObject = JsonObject(emptyMap())
)
