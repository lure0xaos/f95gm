package f95gm.client.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Serializable
internal data class CatalogPayload(val status: String = "", val msg: JsonElement? = null)

@Serializable
internal data class CatalogMessagePayload(
    val data: List<CatalogItemPayload> = emptyList(),
    val pagination: CatalogPaginationPayload? = null
)

@Serializable
internal data class CatalogPaginationPayload(val total: Int = 1)

@Serializable
internal data class CatalogItemPayload(
    @SerialName("thread_id") val threadId: String = "",
    val title: String = "",
    val version: String = "",
    val creator: String = "",
    val developer: String = "",
    val cover: String = "",
    val images: JsonElement = JsonNull,
    val tags: JsonElement = JsonNull,
    val prefixes: JsonElement = JsonNull,
    val date: String = "",
    val likes: String = "0",
    val views: String = "0",
    val rating: JsonElement = JsonNull,
    @SerialName("new") val isNew: JsonElement = JsonNull
)

@Serializable
internal data class CatalogImagesPayload(val cover: String = "")

@Serializable
internal data class DetailPayload(
    val threadId: String = "",
    val title: String = "",
    val creator: String = "",
    val date: String = "",
    val cover: String = "",
    val meta: JsonElement = JsonNull,
    val overviewHtml: String = "",
    val sections: JsonElement = JsonNull,
    val bodyHtml: String = ""
)

@Serializable
internal data class DetailMetaPayload(val label: String = "", val value: String = "")

@Serializable
internal data class DetailSectionPayload(val title: String = "", val html: String = "")

@Serializable
internal data class MarkPayload(
    val threadId: String = "",
    val trackingState: String = "WATCHING",
    val title: String = "",
    val version: String = "",
    val creator: String? = null,
    val developer: String? = null,
    val cover: String = "",
    val prefixes: List<String> = emptyList(),
    val date: String = "",
    val likes: String = "0",
    val views: String = "0",
    val rating: Double = 0.0,
    val isNew: Boolean = false,
    val storedVersion: String = "",
    val latestVersion: String? = null,
    val updateAvailable: Boolean = false,
    val updatedAt: Long = 0L
)

@Serializable
internal data class SavedFilterPayload(
    val id: String = "",
    val name: String = "",
    val category: String = "games",
    val sort: String = "date",
    val search: String = "",
    val creatorSearch: String = "",
    val searchMode: String = "title",
    val selectedTags: List<FilterOptionPayload> = emptyList(),
    val excludedTags: List<FilterOptionPayload> = emptyList(),
    val selectedPrefixes: List<FilterOptionPayload> = emptyList(),
    val excludedPrefixes: List<FilterOptionPayload> = emptyList(),
    val tagType: String = "or",
    val dateLimit: Int = 0,
    val updatedAt: Long = 0L
)

@Serializable
internal data class FilterOptionPayload(val id: String = "", val name: String = "")
