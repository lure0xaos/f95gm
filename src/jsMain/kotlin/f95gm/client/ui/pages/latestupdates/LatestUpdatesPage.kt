package f95gm.client.ui.pages.latestupdates

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.actions.catalog.loading.loadCatalogIfNeeded
import f95gm.client.actions.catalog.loading.loadFilterOptionsIfNeeded
import f95gm.client.actions.catalog.loading.retryCatalog
import f95gm.client.actions.filters.applyFilterRouteIfNeeded
import f95gm.client.actions.filters.loadSavedFiltersIfNeeded
import f95gm.client.state.catalog.catalog
import f95gm.client.state.settings.appSettings
import f95gm.client.ui.catalog.gameCard
import f95gm.client.ui.catalog.pagination.pagination
import f95gm.client.ui.catalog.toolbar.catalogToolbar
import f95gm.client.ui.shared.loading.pageLoadingOverlay
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.latestUpdatesPage(filterId: String? = null) {
    div("catalog-page") {
        catalogToolbar()
        div("feed-status mt-3") {
            catalog.data.map { it.error }.distinctUntilChanged().render { error ->
                if (error.isNotBlank()) {
                    div("feed-error alert alert-danger py-2 d-flex align-items-center justify-content-between gap-3") {
                        span { +error }
                        button("btn btn-sm btn-outline-danger") {
                            type("button")
                            +UiMessages.app_retry()
                            clicks handledBy { retryCatalog() }
                        }
                    }
                }
            }
        }
        pagination()
        div("feed w-100") {
            catalog.data.map { it.items }.distinctUntilChanged().render { items ->
                appSettings.data.map { it.cardSize.gridClass }.render { classNames ->
                    div("game-grid row $classNames g-4 mt-1") {
                        if (items.isEmpty()) {
                            div("empty-column col-12") {
                                div("empty-state card border-secondary-subtle text-center p-5") {
                                    span("empty-icon bi bi-search display-4 text-primary") {}
                                    h2("empty-title h4 mt-3") { +UiMessages.app_noFilterMatches() }
                                    p("empty-hint text-body-secondary") { +UiMessages.app_widerFilters() }
                                }
                            }
                        } else {
                            items.forEach { gameCard(it) }
                        }
                    }
                }
            }
            catalog.data.map { it.loading }.distinctUntilChanged().render { loading ->
                if (loading) pageLoadingOverlay(UiMessages.app_refreshingFeed())
            }
        }
        pagination()
    }
    loadCatalogIfNeeded()
    loadFilterOptionsIfNeeded()
    loadSavedFiltersIfNeeded()
    applyFilterRouteIfNeeded(filterId)
}
