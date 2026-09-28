package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.state.config.ClientConstants
import f95gm.client.ui.catalog.filters.filterField
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.domain.filters.F95SearchMode
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogSearchSection(runtime: ClientCatalogUiPort) {
    val catalog = runtime.catalogView
    catalogFilterCard(UiMessages.app_search()) {
        div("col") {
            filterField(UiMessages.app_searchIn()) {
                bootstrapDropdown(
                    runtime = runtime,
                    selected = catalog.data.map { it.searchMode.wireValue },
                    selectedLabel = catalog.data.map { if (it.searchMode == F95SearchMode.CREATOR) UiMessages.app_creators() else UiMessages.app_titles() },
                    options = flowOf(
                        listOf(
                            DropdownChoice(
                                F95SearchMode.TITLE.wireValue,
                                UiMessages.app_titles()
                            ),
                            DropdownChoice(
                                F95SearchMode.CREATOR.wireValue,
                                UiMessages.app_creators()
                            )
                        )
                    ),
                    buttonClass = "$secondaryNavigationButtonClass text-start",
                    searchKey = ClientConstants.SEARCH_IN_KEY
                ) { runtime.catalogCommands.selectSearchMode(F95SearchMode.fromWire(it)) }
            }
        }
        div("col") {
            filterField(UiMessages.app_creatorSearch()) {
                input("$textFieldClass w-100") {
                    type("search")
                    placeholder(UiMessages.app_searchCreatorsPlaceholder())
                    value(catalog.data.map { it.creatorSearch.value })
                    inputs.values() handledBy { runtime.catalogCommands.updateCreatorSearch(it) }
                }
            }
        }
    }
}
