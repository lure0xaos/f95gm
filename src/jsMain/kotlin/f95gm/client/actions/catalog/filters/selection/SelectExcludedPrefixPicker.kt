package f95gm.client.actions.catalog.filters.selection

import f95gm.client.model.catalog.FilterOptionKind
import f95gm.client.model.catalog.FilterSelection
import f95gm.domain.values.F95OptionId

internal fun selectExcludedPrefixPicker(value: String, groupId: F95OptionId) {
    addSelectedOption(value, FilterSelection.EXCLUDED, FilterOptionKind.PREFIX, groupId)
}
