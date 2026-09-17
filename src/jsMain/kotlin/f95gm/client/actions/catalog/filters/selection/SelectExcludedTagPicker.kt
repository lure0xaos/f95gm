package f95gm.client.actions.catalog.filters.selection

import f95gm.client.model.catalog.FilterOptionKind
import f95gm.client.model.catalog.FilterSelection
import f95gm.client.state.runtime.ClientCatalogActionsPort

internal fun selectExcludedTagPicker(runtime: ClientCatalogActionsPort, value: String) {
    addSelectedOption(runtime, value, FilterSelection.EXCLUDED, FilterOptionKind.TAG)
}
