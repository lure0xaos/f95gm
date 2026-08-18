package f95gm.client.actions.catalog.filters.selection

import f95gm.domain.values.F95OptionId

internal fun selectExcludedPrefixPicker(value: String, groupId: F95OptionId) {
    addSelectedOption(value, excluded = true, prefix = true, groupId = groupId)
}
