package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.state.config.ClientConstants
import f95gm.client.state.options.dateLimitChoices
import f95gm.client.state.options.dateLimitLabel
import f95gm.client.ui.catalog.filters.filterField
import f95gm.client.ui.catalog.filters.filterPicker
import f95gm.client.ui.navigation.NavbarPage
import f95gm.client.ui.navigation.responsiveNavbar
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.navigationButtonClass
import f95gm.client.ui.shared.navigation.primaryNavigationButtonClass
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.client.state.runtime.ClientCatalogPagePort
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95TagType
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogToolbar(runtime: ClientCatalogPagePort) {
    val catalog = runtime.catalogView
    val catalogOptions = runtime.catalogOptionsView
    val filterPanelOpen = runtime.filterPanelOpenView
    responsiveNavbar(
        runtime = runtime,
        navigationId = "catalog-navigation",
        page = NavbarPage.CATALOG,
        toolbar = { catalogToolbarNavigation(runtime) },
        trailingAction = {
            button("filter-toggle") {
                className(filterPanelOpen.data.map { isOpen ->
                    "filter-toggle ${if (isOpen) primaryNavigationButtonClass else navigationButtonClass}"
                })
                type("button")
                attr("data-bs-toggle", "collapse")
                attr("data-bs-target", "#catalog-filter-panel")
                attr("aria-controls", "catalog-filter-panel")
                attr("aria-expanded", "false")
                span("bi bi-funnel me-1") {}
                +UiMessages.app_filters()
                clicks handledBy { runtime.catalogCommands.toggleFilterPanel() }
            }
        }
    )
    catalogToolbarFilters(runtime)
}
