package f95gm.client.actions.catalog.filters.selection

import f95gm.client.model.catalog.FilterOptionKind
import f95gm.client.model.catalog.FilterSelection
import f95gm.domain.values.F95OptionId
import f95gm.client.state.runtime.ClientCatalogActionsPort

internal fun selectExcludedPrefixPicker(runtime: ClientCatalogActionsPort, value: String, groupId: F95OptionId) {
    addSelectedOption(runtime, value, FilterSelection.EXCLUDED, FilterOptionKind.PREFIX, groupId)
}
