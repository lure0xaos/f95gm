package f95gm.client.data.catalog

import f95gm.client.model.catalog.CatalogOptions
import f95gm.client.model.catalog.TagOption
import f95gm.client.data.api.PayloadParseException
import f95gm.client.data.api.PayloadParseResult
import f95gm.client.data.api.PayloadPolicies
import f95gm.client.data.api.parsePayload
import f95gm.client.model.runtime.EndpointId
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95TagId

internal fun parseCatalogOptions(payload: String): CatalogOptions = when (val result = parseCatalogOptionsResult(payload)) {
    is PayloadParseResult.Success -> result.value
    is PayloadParseResult.Failure -> throw PayloadParseException(
        result.reason,
        "Catalog options payload is invalid",
        endpoint = EndpointId("catalog-options")
    )
}

internal fun parseCatalogOptionsResult(payload: String): PayloadParseResult<CatalogOptions> =
    parsePayload<CatalogOptionsPayload, CatalogOptions>(payload, policy = PayloadPolicies.Default) { root ->
    val typed = root.toValues()
    val tags = typed.tags.map { (id, name) -> TagOption(F95TagId(id), F95OptionName(name)) }
        .sortedBy { it.name.value.lowercase() }
    val prefixGroups = parsePrefixGroups(typed.prefixes)
    val prefixes = prefixGroups
        .flatMap { it.options }
        .distinctBy { it.id }
        .sortedBy { it.name.value.lowercase() }
    CatalogOptions(tags = tags, prefixes = prefixes, prefixGroups = prefixGroups)
}
