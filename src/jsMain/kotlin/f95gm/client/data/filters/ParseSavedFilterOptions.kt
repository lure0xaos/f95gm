package f95gm.client.data.filters

import f95gm.client.data.json.string
import f95gm.client.model.catalog.TagOption
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95TagId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal fun parseSavedTagOptions(item: JsonObject, key: String): List<TagOption> =
    item[key]?.jsonArray.orEmpty().mapNotNull { element ->
        val option = element.jsonObject
        val id = option.string("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        TagOption(F95TagId(id), F95OptionName(option.string("name", id)))
    }
