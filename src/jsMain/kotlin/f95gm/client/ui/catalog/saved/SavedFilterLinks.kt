package f95gm.client.ui.catalog.saved

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.routing.navigation.filterRoute
import f95gm.client.routing.navigation.router
import f95gm.client.routing.pages.routeFilterId
import f95gm.client.state.filters.savedFilters
import f95gm.client.ui.shared.navigation.quickLinkNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.savedFilterLinks() {
    savedFilters.data.map { it.items }.distinctUntilChanged()
        .combine(router.data.map { it[routeFilterId] }.distinctUntilChanged()) { items, activeFilterId ->
            items to activeFilterId
        }.render { (items, activeFilterId) ->
            if (items.isNotEmpty()) {
                div("quick-links dropdown") {
                    button("quick-toggle $quickLinkNavigationButtonClass dropdown-toggle") {
                        type("button")
                        attr("data-bs-toggle", "dropdown")
                        attr("aria-expanded", "false")
                        span("bi bi-link-45deg me-1") {}
                        +(items.firstOrNull { it.id.value == activeFilterId }?.name?.value
                            ?: UiMessages.app_savedQuickLinks())
                    }
                    ul("quick-options dropdown-menu dropdown-menu-end p-2") {
                        items.forEach { filter ->
                            li("quick-item") {
                                a("quick-link $quickLinkNavigationButtonClass text-start text-nowrap") {
                                    clicks.map { filterRoute(filter.id.value) } handledBy router.navTo
                                    +filter.name.value
                                }
                            }
                        }
                    }
                }
            }
        }
}
