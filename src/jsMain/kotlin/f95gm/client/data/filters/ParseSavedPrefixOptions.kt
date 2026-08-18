package f95gm.client.data.filters

import f95gm.client.data.json.string
import f95gm.client.model.catalog.PrefixOption
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95PrefixId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal fun parseSavedPrefixOptions(item: JsonObject, key: String): List<PrefixOption> =
    item[key]?.jsonArray.orEmpty().mapNotNull { element ->
        val option = element.jsonObject
        val id = option.string("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        PrefixOption(F95PrefixId(id), F95OptionName(option.string("name", id)))
    }
