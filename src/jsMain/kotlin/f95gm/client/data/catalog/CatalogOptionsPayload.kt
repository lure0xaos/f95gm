package f95gm.client.data.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Tolerant envelope for the upstream options response; irregular fields stay JSON until normalized. */
@Serializable
internal data class CatalogOptionsPayload(
    val tags: JsonElement? = null,
    val prefixes: JsonElement? = null
)

internal data class CatalogOptionsPayloadValues(
    val tags: Map<String, String>,
    val prefixes: JsonElement?
)

/** Converts irregular wire JSON into the typed portion consumed by the catalog parser. */
internal fun CatalogOptionsPayload.toValues(): CatalogOptionsPayloadValues =
    CatalogOptionsPayloadValues(
        tags = (tags as? JsonObject).orEmpty().mapNotNull { (id, value) ->
            (value as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)?.let { id to it }
        }.toMap(),
        prefixes = prefixes
    )
