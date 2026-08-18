package f95gm.client.data.detail

import f95gm.client.data.catalog.localizedDetailLabel
import f95gm.client.data.json.string
import f95gm.client.model.detail.DetailMeta
import f95gm.client.model.detail.DetailSection
import f95gm.client.model.detail.DetailState
import f95gm.domain.values.*
import f95gm.messages.UiMessages
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal fun parseDetail(payload: String): DetailState {
    val item = Json.parseToJsonElement(payload).jsonObject
    return DetailState(
        threadId = F95ThreadId(item.string("threadId")),
        title = F95GameTitle(item.string("title", UiMessages.app_untitledItem())),
        creator = F95DeveloperName(item.string("creator")),
        date = F95DateLabel(item.string("date")),
        cover = F95CoverUrl(item.string("cover")),
        meta = item["meta"]?.jsonArray?.map { meta ->
            val value = meta.jsonObject
            DetailMeta(F95OptionName(localizedDetailLabel(value.string("label"))), F95OptionName(value.string("value")))
        }.orEmpty(),
        overviewHtml = F95Html(item.string("overviewHtml")),
        sections = item["sections"]?.jsonArray?.map { section ->
            val value = section.jsonObject
            DetailSection(F95OptionName(value.string("title")), F95Html(value.string("html")))
        }.orEmpty(),
        bodyHtml = F95Html(item.string("bodyHtml"))
    )
}
