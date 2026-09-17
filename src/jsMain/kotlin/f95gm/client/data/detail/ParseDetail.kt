package f95gm.client.data.detail

import f95gm.client.data.catalog.localizedDetailLabel
import f95gm.client.data.api.DetailPayload
import f95gm.client.data.api.PayloadParseResult
import f95gm.client.data.api.ParseFailureReason
import f95gm.client.data.api.parsePayload
import f95gm.client.data.api.PayloadParseException
import f95gm.client.data.api.PayloadPolicies
import f95gm.client.data.api.toValues
import f95gm.client.model.runtime.EndpointId
import f95gm.client.state.runtime.clientJson
import f95gm.client.model.detail.DetailMeta
import f95gm.client.model.detail.DetailSection
import f95gm.client.model.detail.DetailState
import f95gm.domain.values.*
import f95gm.messages.UiMessages
import kotlinx.serialization.decodeFromString

internal fun parseDetail(payload: String): DetailState = when (val result = parseDetailResult(payload)) {
    is PayloadParseResult.Success -> result.value
    is PayloadParseResult.Failure -> throw PayloadParseException(result.reason, "Detail payload is invalid", endpoint = EndpointId("detail"))
}

internal fun parseDetailResult(payload: String): PayloadParseResult<DetailState> = parsePayload<DetailPayload, DetailState>(payload, policy = PayloadPolicies.Detail) { value ->
    value.toValues().let { typed -> DetailState(
        threadId = F95ThreadId(typed.threadId),
        title = F95GameTitle(typed.title.ifBlank { UiMessages.app_untitledItem() }),
        creator = F95DeveloperName(typed.creator),
        date = F95DateLabel(typed.date),
        cover = F95CoverUrl(typed.cover),
        meta = typed.meta.map { item ->
            DetailMeta(F95OptionName(localizedDetailLabel(item.label)), F95OptionName(item.value))
        },
        overviewHtml = F95Html(typed.overviewHtml),
        sections = typed.sections.map { section ->
            DetailSection(F95OptionName(section.title), F95Html(section.html))
        },
        bodyHtml = F95Html(typed.bodyHtml)
    )
    }
}
