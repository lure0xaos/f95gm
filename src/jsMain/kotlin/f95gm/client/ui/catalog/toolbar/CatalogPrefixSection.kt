package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.state.dropdown.DropdownSearchKeys
import f95gm.client.ui.catalog.filters.filterPicker
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import f95gm.client.model.catalog.FilterGroup

internal fun RenderContext.catalogPrefixSection(runtime: ClientCatalogUiPort, group: FilterGroup) {
    val catalog = runtime.catalogView
    catalogFilterCard(group.name.value) {
        filterPicker(
            runtime = runtime,
            fieldLabel = UiMessages.app_includeGroup(group.name.value),
            options = flowOf(group.options),
            selected = catalog.data.map { it.prefixPickers[group.id].orEmpty() },
            highlighted = catalog.data.map { state ->
                state.selectedPrefixes.map { it.id.value }.toSet()
            },
            searchKey = DropdownSearchKeys.includePrefix(group.id),
            onChange = { runtime.catalogCommands.selectPrefixPicker(it, group.id) }
        )
        filterPicker(
            runtime = runtime,
            fieldLabel = UiMessages.app_excludeGroup(group.name.value),
            options = flowOf(group.options),
            selected = catalog.data.map { it.excludedPrefixPickers[group.id].orEmpty() },
            highlighted = catalog.data.map { state ->
                state.excludedPrefixes.map { it.id.value }.toSet()
            },
            searchKey = DropdownSearchKeys.excludePrefix(group.id),
            onChange = { runtime.catalogCommands.selectExcludedPrefixPicker(it, group.id) }
        )
    }
}
