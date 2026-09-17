package f95gm.client.data.api

import f95gm.client.data.json.arrayStrings
import f95gm.client.data.json.doubleValue
import f95gm.client.data.json.objectArray
import f95gm.client.data.json.stringValue
import f95gm.client.data.json.booleanValue
import kotlinx.serialization.json.JsonElement

/** Typed application-facing view of legacy catalog fields whose wire types vary upstream. */
internal data class CatalogItemValues(
    val threadId: String,
    val title: String,
    val version: String,
    val developer: String,
    val cover: String,
    val tags: List<String>,
    val prefixes: List<String>,
    val date: String,
    val likes: String,
    val views: String,
    val rating: Double?,
    val isNew: Boolean
)

internal fun CatalogItemPayload.toValues(): CatalogItemValues = CatalogItemValues(
    threadId = threadId,
    title = title,
    version = version,
    developer = creator.ifBlank { developer },
    cover = cover.ifBlank { images.stringValue("cover") },
    tags = tags.arrayStrings(),
    prefixes = prefixes.arrayStrings(),
    date = date,
    likes = likes,
    views = views,
    rating = rating.doubleValue(),
    isNew = isNew.booleanValue() == true
)

internal data class DetailPayloadValues(
    val threadId: String,
    val title: String,
    val creator: String,
    val date: String,
    val cover: String,
    val meta: List<DetailMetaPayload>,
    val overviewHtml: String,
    val sections: List<DetailSectionPayload>,
    val bodyHtml: String
)

internal fun DetailPayload.toValues(): DetailPayloadValues = DetailPayloadValues(
    threadId = threadId,
    title = title,
    creator = creator,
    date = date,
    cover = cover,
    meta = meta.objectArray().map {
        DetailMetaPayload(label = it.stringValue("label"), value = it.stringValue("value"))
    },
    overviewHtml = overviewHtml,
    sections = sections.objectArray().map {
        DetailSectionPayload(title = it.stringValue("title"), html = it.stringValue("html"))
    },
    bodyHtml = bodyHtml
)
