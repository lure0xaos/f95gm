package f95gm.client.actions.catalog.filters.selection

internal fun selectTagPicker(value: String) {
    addSelectedOption(value, excluded = false, prefix = false)
}
