package f95gm.client.ui.shared.dropdown

import dev.fritz2.core.*
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.state.runtime.ClientDropdownUiPort
import f95gm.client.state.dropdown.DropdownSearchKey
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun <T> RenderContext.bootstrapDropdown(
    runtime: ClientDropdownUiPort,
    selected: Flow<T>,
    selectedLabel: Flow<String>,
    options: Flow<List<DropdownChoice<T>>>,
    buttonClass: String = secondaryNavigationButtonClass,
    autoClose: String = "true",
    highlighted: Flow<Set<T>> = selected.map { setOf(it) },
    searchKey: DropdownSearchKey? = null,
    widthClass: String = "w-100",
    onChange: (T) -> Unit
) {
    val searchQuery = searchKey?.let { key -> runtime.dropdownSearchesView.data.map { it[key] } } ?: flowOf("")
    val visibleOptions = options.combine(searchQuery) { choices, query ->
        choices.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val buttonWidthClass = if (widthClass == "w-100") " w-100" else ""
    val dropdownMenuClass = searchQuery.map { query ->
        "dropdown-options dropdown-menu overflow-auto $widthClass" + if (query.isNotEmpty()) " show" else ""
    }
    div("dropdown-field " + if (searchKey != null) "input-group flex-nowrap dropdown position-relative $widthClass" else "dropdown position-relative $widthClass") {
        if (searchKey != null) {
            keyupsIf {
                if (shortcutOf(this) in setOf(Keys.Tab, Keys.Shift + Keys.Tab)) {
                    stopPropagation()
                    true
                } else false
            } handledBy { }
        }
        if (searchKey != null) {
            input("dropdown-search $textFieldClass") {
                type("search")
                attr("role", "combobox")
                attr("aria-autocomplete", "list")
                placeholder(UiMessages.app_filterOptionsPlaceholder())
                value(searchQuery)
                inputs.values() handledBy { runtime.dropdownCommands.updateDropdownSearch(searchKey, it) }
                clicks { stopPropagation() } handledBy { }
            }
            button("dropdown-button $buttonClass dropdown-toggle text-truncate text-nowrap flex-shrink-1 overflow-hidden") {
                type("button")
                attr("data-bs-toggle", "dropdown")
                attr("data-bs-auto-close", autoClose)
                attr("aria-expanded", searchQuery.map { (it.isNotEmpty()).toString() })
                selectedLabel.render { +it }
            }
        } else {
            button("dropdown-button $buttonClass dropdown-toggle text-start text-truncate text-nowrap$buttonWidthClass") {
                type("button")
                attr("data-bs-toggle", "dropdown")
                attr("data-bs-auto-close", autoClose)
                attr("aria-expanded", "false")
                selectedLabel.render { +it }
            }
        }
        ul("dropdown-menu-options") {
            className(dropdownMenuClass)
            visibleOptions.render { choices ->
                if (choices.isEmpty()) {
                    li("dropdown-empty dropdown-item-text small text-body-secondary") { +UiMessages.app_noOptionsMatch() }
                }
                choices.forEach { choice ->
                    li {
                        button {
                            type("button")
                            className(highlighted.map { values ->
                                if (choice.value in values) "dropdown-item text-wrap text-uppercase active"
                                else "dropdown-item text-wrap text-uppercase"
                            })
                            +choice.label
                            clicks handledBy { onChange(choice.value) }
                        }
                    }
                }
            }
        }
    }
}
