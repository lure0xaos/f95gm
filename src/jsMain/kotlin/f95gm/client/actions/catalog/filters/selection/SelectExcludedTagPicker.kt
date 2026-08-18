package f95gm.client.actions.catalog.filters.selection

internal fun selectExcludedTagPicker(value: String) {
    addSelectedOption(value, excluded = true, prefix = false)
}
