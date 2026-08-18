package f95gm.client.data.catalog

import f95gm.client.model.catalog.FilterGroup
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import f95gm.messages.UiMessages
import kotlinx.serialization.json.*

internal fun parsePrefixGroups(element: JsonElement?): List<FilterGroup> {
    val groupElements = when (element) {
        is JsonArray -> element
        is JsonObject -> element.values.filterIsInstance<JsonArray>()
            .firstOrNull() ?: emptyList()

        else -> emptyList()
    }
    val groups = groupElements.mapIndexedNotNull { index, groupElement ->
        val group = groupElement as? JsonObject ?: return@mapIndexedNotNull null
        val nested = sequenceOf("prefixes", "items", "children").firstNotNullOfOrNull { group[it] as? JsonArray }
        val options = nested
            ?.flatMap(::prefixOptions)
            ?.distinctBy { it.id }
            ?.sortedBy { it.name.value.lowercase() }
            .orEmpty()
        if (options.isEmpty()) return@mapIndexedNotNull null
        val name = group["name"]?.jsonPrimitive?.contentOrNull
            ?: group["title"]?.jsonPrimitive?.contentOrNull
            ?: UiMessages.app_prefixes()
        val id = group["id"]?.jsonPrimitive?.contentOrNull
            ?: group["group_id"]?.jsonPrimitive?.contentOrNull
            ?: "$index:$name"
        FilterGroup(id = F95OptionId(id), name = F95OptionName(name), options = options)
    }
    if (groups.isNotEmpty()) return groups

    val options = groupElements
        .flatMap(::prefixOptions)
        .distinctBy { it.id }
        .sortedBy { it.name.value.lowercase() }
    return if (options.isEmpty()) emptyList() else listOf(
        FilterGroup(
            F95OptionId("prefixes"),
            F95OptionName(UiMessages.app_prefixes()),
            options
        )
    )
}
