package f95gm.server.routes.marks.live

import kotlinx.serialization.json.JsonObject
import f95gm.domain.json.jsonBooleanOrNull
import f95gm.domain.json.jsonDoubleOrNull
import f95gm.domain.json.jsonStringList
import f95gm.domain.json.jsonText

/** Parsed domain-shaped values; callers do not need to know the upstream JSON representation. */
internal data class UpstreamCatalogItemValues(
    val threadId: String,
    val title: String,
    val version: String,
    val developer: String,
    val cover: String,
    val prefixes: List<String>,
    val date: String,
    val likes: String,
    val views: String,
    val rating: Double,
    val isNew: Boolean
)

internal fun UpstreamCatalogItem.toValues(): UpstreamCatalogItemValues = UpstreamCatalogItemValues(
    threadId = threadId.jsonText(),
    title = title.jsonText(),
    version = version.jsonText(),
    developer = creator.jsonText().ifBlank { developer.jsonText() },
    cover = cover.jsonText().ifBlank { (images as? JsonObject)?.get("cover")?.jsonText().orEmpty() },
    prefixes = prefixes.jsonStringList(),
    date = date.jsonText(),
    likes = likes.jsonText(),
    views = views.jsonText(),
    rating = rating.jsonDoubleOrNull() ?: 0.0,
    isNew = isNew.jsonBooleanOrNull() == true
)
