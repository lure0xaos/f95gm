package f95gm.server.model.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class CatalogOptionsResponse(val tags: JsonObject, val prefixes: JsonElement)
