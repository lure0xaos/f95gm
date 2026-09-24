package f95gm.client.actions.catalog.filters.selection

import f95gm.client.model.catalog.FilterOptionKind
import f95gm.client.model.catalog.FilterSelection

internal fun selectTagPicker(value: String) {
    addSelectedOption(value, FilterSelection.INCLUDED, FilterOptionKind.TAG)
}
