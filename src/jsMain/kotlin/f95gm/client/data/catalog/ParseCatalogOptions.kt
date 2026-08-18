package f95gm.client.data.catalog

import f95gm.client.model.catalog.CatalogOptions
import f95gm.client.model.catalog.TagOption
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95TagId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun parseCatalogOptions(payload: String): CatalogOptions {
    val root = Json.parseToJsonElement(payload).jsonObject
    val tags = root["tags"]?.jsonObject.orEmpty()
        .map { (id, value) ->
            TagOption(
                F95TagId(id),
                F95OptionName(value.jsonPrimitive.contentOrNull.orEmpty())
            )
        }
        .filter { it.name.value.isNotBlank() }
        .sortedBy { it.name.value.lowercase() }
    val prefixGroups = parsePrefixGroups(root["prefixes"])
    val prefixes = prefixGroups
        .flatMap { it.options }
        .distinctBy { it.id }
        .sortedBy { it.name.value.lowercase() }
    return CatalogOptions(tags = tags, prefixes = prefixes, prefixGroups = prefixGroups)
}
