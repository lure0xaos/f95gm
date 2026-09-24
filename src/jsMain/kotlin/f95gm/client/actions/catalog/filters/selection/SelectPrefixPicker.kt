package f95gm.client.actions.catalog.filters.selection

import f95gm.client.model.catalog.FilterOptionKind
import f95gm.client.model.catalog.FilterSelection
import f95gm.domain.values.F95OptionId

internal fun selectPrefixPicker(value: String, groupId: F95OptionId) {
    addSelectedOption(value, FilterSelection.INCLUDED, FilterOptionKind.PREFIX, groupId)
}
