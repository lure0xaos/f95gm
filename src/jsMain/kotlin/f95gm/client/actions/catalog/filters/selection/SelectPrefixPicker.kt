package f95gm.client.actions.catalog.filters.selection

import f95gm.domain.values.F95OptionId

internal fun selectPrefixPicker(value: String, groupId: F95OptionId) {
    addSelectedOption(value, excluded = false, prefix = true, groupId = groupId)
}
