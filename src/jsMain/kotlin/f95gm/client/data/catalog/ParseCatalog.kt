package f95gm.client.data.catalog

import f95gm.client.data.api.apiErrorFallback
import f95gm.client.data.api.CatalogItemPayload
import f95gm.client.data.api.CatalogMessagePayload
import f95gm.client.data.api.CatalogPayload
import f95gm.client.data.api.PayloadParseResult
import f95gm.client.data.api.ParseFailureReason
import f95gm.client.data.api.parsePayload
import f95gm.client.data.api.PayloadParseException
import f95gm.client.data.api.PayloadPolicies
import f95gm.client.data.api.PayloadEnvelopeException
import f95gm.client.data.api.PayloadMappingException
import f95gm.client.data.api.toValues
import f95gm.client.model.runtime.EndpointId
import f95gm.client.state.runtime.clientJson
import f95gm.client.data.json.*
import f95gm.client.model.catalog.Game
import f95gm.client.state.config.ClientConstants
import f95gm.domain.api.F95JsonKeys
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.*
import f95gm.messages.UiMessages
import kotlinx.serialization.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerializationException

internal fun parseCatalog(payload: String): CatalogResult {
    return when (val result = parseCatalogResult(payload)) {
    is PayloadParseResult.Success -> result.value
    is PayloadParseResult.Failure -> throw PayloadParseException(result.reason, "Catalog payload is invalid", endpoint = EndpointId("catalog"))
    }
}

internal fun parseCatalogResult(payload: String): PayloadParseResult<CatalogResult> =
    parsePayload<CatalogPayload, CatalogResult>(payload, policy = PayloadPolicies.Catalog) { root ->
        if (root.status != F95Defaults.API_OK) {
            throw PayloadEnvelopeException(root.msg?.jsonPrimitive?.contentOrNull ?: apiErrorFallback)
        }
        val messagePayload = root.msg?.toString() ?: throw PayloadEnvelopeException(apiErrorFallback)
        val message = try {
            clientJson.decodeFromString<CatalogMessagePayload>(messagePayload)
        } catch (failure: SerializationException) {
            throw PayloadMappingException(ParseFailureReason.INVALID_PAYLOAD, failure)
        }
        CatalogResult(message.data.map(::toGame), message.pagination?.total?.coerceAtLeast(F95Defaults.FIRST_PAGE) ?: F95Defaults.FIRST_PAGE)
    }

private fun toGame(item: CatalogItemPayload): Game = item.toValues().let { value -> Game(
    threadId = F95ThreadId(value.threadId),
    title = F95GameTitle(value.title.ifBlank { UiMessages.app_untitledRelease() }),
    version = F95GameVersion(value.version.ifBlank { UiMessages.app_versionUnknown() }),
    developer = F95DeveloperName(value.developer),
    cover = F95CoverUrl(value.cover),
    tags = value.tags.map(::F95TagId),
    prefixes = value.prefixes.map(::F95PrefixId),
    date = F95DateLabel(value.date.ifBlank { UiMessages.app_recently() }),
    likes = F95MetricText(value.likes),
    views = F95MetricText(value.views),
    rating = value.rating ?: ClientConstants.NO_RATING,
    isNew = value.isNew
)}
