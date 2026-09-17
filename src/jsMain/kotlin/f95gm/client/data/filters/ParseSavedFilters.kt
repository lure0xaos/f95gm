package f95gm.client.data.filters

import f95gm.client.data.api.SavedFilterPayload
import f95gm.client.data.api.FilterOptionPayload
import f95gm.client.model.catalog.PrefixOption
import f95gm.client.model.catalog.TagOption
import f95gm.client.model.filters.SavedFilter
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95EpochMillis
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionName
import f95gm.messages.UiMessages
import f95gm.client.state.runtime.clientJson
import f95gm.client.data.api.parsePayloadOrThrow
import f95gm.domain.values.F95PrefixId
import f95gm.domain.values.F95TagId

internal fun parseSavedFilters(payload: String): List<SavedFilter> =
    parsePayloadOrThrow<List<SavedFilterPayload>, List<SavedFilter>>(payload) { filters -> filters.map { item ->
        SavedFilter(
            id = F95FilterId(item.id),
            name = F95OptionName(item.name.ifBlank { UiMessages.app_unnamedFilter() }),
            category = F95Category.fromWire(item.category),
            sort = F95Sort.fromWire(item.sort),
            search = item.search,
            creatorSearch = item.creatorSearch,
            searchMode = F95SearchMode.fromWire(item.searchMode),
            selectedTags = item.selectedTags.mapNotNull { it.toTagOption() },
            excludedTags = item.excludedTags.mapNotNull { it.toTagOption() },
            selectedPrefixes = item.selectedPrefixes.mapNotNull { it.toPrefixOption() },
            excludedPrefixes = item.excludedPrefixes.mapNotNull { it.toPrefixOption() },
            tagType = F95TagType.fromWire(item.tagType),
            dateLimit = item.dateLimit,
            updatedAt = F95EpochMillis(item.updatedAt)
        )
    } }

private fun FilterOptionPayload.toTagOption(): TagOption? = id.takeIf { it.isNotBlank() }?.let {
    TagOption(F95TagId(it), F95OptionName(name.ifBlank { it }))
}

private fun FilterOptionPayload.toPrefixOption(): PrefixOption? = id.takeIf { it.isNotBlank() }?.let {
    PrefixOption(F95PrefixId(it), F95OptionName(name.ifBlank { it }))
}
