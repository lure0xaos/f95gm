package f95gm.client.ui.catalog.filters

import dev.fritz2.core.RenderContext
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.model.catalog.FilterChoice
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.client.state.dropdown.DropdownSearchKey
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun <T : FilterChoice> RenderContext.filterPicker(
    runtime: ClientCatalogUiPort,
    fieldLabel: String,
    options: Flow<List<T>>,
    selected: Flow<String>,
    highlighted: Flow<Set<String>>,
    searchKey: DropdownSearchKey,
    columnClass: String = "col",
    onChange: (String) -> Unit
) {
    div("filter-picker $columnClass") {
        filterField(fieldLabel) {
            options.render { available ->
                bootstrapDropdown(
                    runtime = runtime,
                    selected = selected,
                    selectedLabel = selected.map { value ->
                        available.firstOrNull { it.value == value }?.label ?: UiMessages.app_choose()
                    },
                    options = flowOf(available.map { DropdownChoice(it.value, it.label) }),
                    buttonClass = "$secondaryNavigationButtonClass text-start",
                    autoClose = "outside",
                    highlighted = highlighted,
                    searchKey = searchKey
                ) { onChange(it) }
            }
        }
    }
}
