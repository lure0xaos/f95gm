package f95gm.client.data.catalog

import f95gm.client.data.api.apiErrorFallback
import f95gm.client.data.json.int
import f95gm.client.data.json.string
import f95gm.client.model.catalog.Game
import f95gm.client.state.config.ClientConstants
import f95gm.domain.api.F95JsonKeys
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.*
import f95gm.messages.UiMessages
import kotlinx.serialization.json.*

internal fun parseCatalog(payload: String): CatalogResult {
    val root = Json.parseToJsonElement(payload).jsonObject
    if (root[F95JsonKeys.STATUS]?.jsonPrimitive?.contentOrNull != F95Defaults.API_OK) {
        val message = root["msg"]?.jsonPrimitive?.contentOrNull ?: apiErrorFallback
        error(message)
    }
    val msg = root["msg"]?.jsonObject ?: error(apiErrorFallback)
    val data = msg["data"]?.jsonArray.orEmpty().map { json ->
        val item = json.jsonObject
        Game(
            threadId = F95ThreadId(item.string("thread_id")),
            title = F95GameTitle(item.string("title", UiMessages.app_untitledRelease())),
            version = F95GameVersion(item.string("version", UiMessages.app_versionUnknown())),
            developer = F95DeveloperName(item.string("creator").ifBlank { item.string("developer") }),
            cover = F95CoverUrl(item.string("cover").ifBlank { item["images"]?.jsonObject?.string("cover").orEmpty() }),
            tags = item["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                .map(::F95TagId),
            prefixes = item["prefixes"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
                .orEmpty().map(::F95PrefixId),
            date = F95DateLabel(item.string("date", UiMessages.app_recently())),
            likes = F95MetricText(item.string("likes", F95Defaults.ZERO_METRIC)),
            views = F95MetricText(item.string("views", F95Defaults.ZERO_METRIC)),
            rating = item["rating"]?.jsonPrimitive?.doubleOrNull ?: ClientConstants.NO_RATING,
            isNew = item["new"]?.jsonPrimitive?.booleanOrNull == true
        )
    }
    val pagination = msg["pagination"]?.jsonObject
    return CatalogResult(
        data, pagination?.int("total", F95Defaults.FIRST_PAGE)
            ?.coerceAtLeast(F95Defaults.FIRST_PAGE) ?: F95Defaults.FIRST_PAGE
    )
}
