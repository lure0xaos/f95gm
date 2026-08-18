package f95gm.client.data.filters

import f95gm.client.data.json.string
import f95gm.client.model.filters.SavedFilter
import f95gm.domain.api.F95Api
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionName
import f95gm.messages.UiMessages
import kotlinx.serialization.json.*

internal fun parseSavedFilters(payload: String): List<SavedFilter> =
    Json.parseToJsonElement(payload).jsonArray.map { element ->
        val item = element.jsonObject
        SavedFilter(
            id = F95FilterId(item.string("id")),
            name = F95OptionName(item.string("name", UiMessages.app_unnamedFilter())),
            category = F95Category.fromWire(item.string(F95Api.CATEGORY, F95Defaults.DEFAULT_CATEGORY)),
            sort = F95Sort.fromWire(item.string(F95Api.SORT, F95Defaults.DEFAULT_SORT)),
            search = item.string("search"),
            creatorSearch = item.string("creatorSearch"),
            searchMode = F95SearchMode.fromWire(item.string("searchMode", F95Defaults.DEFAULT_SEARCH_MODE)),
            selectedTags = parseSavedTagOptions(item, "selectedTags"),
            excludedTags = parseSavedTagOptions(item, "excludedTags"),
            selectedPrefixes = parseSavedPrefixOptions(item, "selectedPrefixes"),
            excludedPrefixes = parseSavedPrefixOptions(item, "excludedPrefixes"),
            tagType = F95TagType.fromWire(item.string("tagType", F95Defaults.DEFAULT_TAG_TYPE)),
            dateLimit = item["dateLimit"]?.jsonPrimitive?.intOrNull ?: F95Defaults.ANY_DATE,
            updatedAt = item["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L
        )
    }
