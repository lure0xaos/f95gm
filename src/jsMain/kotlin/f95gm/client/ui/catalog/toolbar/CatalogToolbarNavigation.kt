package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.navigationButtonClass
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95Sort
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogToolbarNavigation(runtime: ClientCatalogUiPort) {
    val catalog = runtime.catalogView
    div("catalog-tools d-flex flex-wrap flex-xl-nowrap align-items-center gap-2 w-100") {
        val categoryChoices = catalogCategoryChoices()
        bootstrapDropdown(
            runtime = runtime,
            selected = catalog.data.map { it.category.wireValue },
            selectedLabel = catalog.data.map { state ->
                categoryChoices.firstOrNull { it.value == state.category.wireValue }?.label
                    ?: UiMessages.app_games()
            },
            options = flowOf(categoryChoices),
            buttonClass = navigationButtonClass,
            widthClass = "w-auto"
        ) { runtime.catalogCommands.selectCategory(F95Category.fromWire(it)) }
        div("catalog-search input-group flex-grow-1 flex-shrink-1") {
            span("search-icon $secondaryNavigationButtonClass rounded-0 rounded-start") {
                attr("aria-hidden", "true")
                span("bi bi-search") {}
            }
            input("search-input $textFieldClass flex-grow-1") {
                type("search")
                placeholder(UiMessages.app_searchTitlePlaceholder())
                value(catalog.data.map { it.search.value })
                inputs.values() handledBy { runtime.catalogCommands.updateCatalogSearch(it) }
            }
            bootstrapDropdown(
                runtime = runtime,
                selected = catalog.data.map { it.sort.wireValue },
                selectedLabel = catalog.data.map { catalogSortLabel(it.sort) },
                options = flowOf(catalogSortChoices()),
                buttonClass = "$secondaryNavigationButtonClass rounded-0 rounded-end",
                widthClass = "w-auto"
            ) { runtime.catalogCommands.selectSort(F95Sort.fromWire(it)) }
        }
    }
}
