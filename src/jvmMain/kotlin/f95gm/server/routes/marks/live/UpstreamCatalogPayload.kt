package f95gm.server.routes.marks.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/** Tolerant DTO for the legacy catalog response; wire irregularities stay at this boundary. */
@Serializable
internal data class UpstreamCatalogPayload(val msg: UpstreamCatalogMessage? = null)

@Serializable
internal data class UpstreamCatalogMessage(val data: List<UpstreamCatalogItem> = emptyList())

@Serializable
internal data class UpstreamCatalogItem(
    @SerialName("thread_id") val threadId: JsonElement = JsonNull,
    val title: JsonElement = JsonNull,
    val version: JsonElement = JsonNull,
    val creator: JsonElement = JsonNull,
    val developer: JsonElement = JsonNull,
    val cover: JsonElement = JsonNull,
    val images: JsonElement = JsonNull,
    val prefixes: JsonElement = JsonNull,
    val date: JsonElement = JsonNull,
    val likes: JsonElement = JsonNull,
    val views: JsonElement = JsonNull,
    val rating: JsonElement = JsonNull,
    @SerialName("new") val isNew: JsonElement = JsonNull
)
