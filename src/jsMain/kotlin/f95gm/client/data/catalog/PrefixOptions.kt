package f95gm.client.data.catalog

import f95gm.client.model.catalog.PrefixOption
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95PrefixId
import kotlinx.serialization.json.*

internal fun prefixOptions(element: JsonElement): List<PrefixOption> {
    val objectValue = element as? JsonObject ?: return emptyList()
    val children = sequenceOf("prefixes", "items", "children").firstNotNullOfOrNull { objectValue[it] as? JsonArray }
        ?.flatMap(::prefixOptions)
        .orEmpty()
    if (children.isNotEmpty()) return children
    val id = objectValue["id"]?.jsonPrimitive?.contentOrNull ?: return emptyList()
    val name = objectValue["name"]?.jsonPrimitive?.contentOrNull
        ?: objectValue["title"]?.jsonPrimitive?.contentOrNull
        ?: return emptyList()
    return listOf(PrefixOption(F95PrefixId(id), F95OptionName(name)))
}
