package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.client.ui.shared.navigation.primaryNavigationButtonClass
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages

internal fun RenderContext.catalogFilterHeader(runtime: ClientCatalogUiPort) {
    val catalog = runtime.catalogView
    div("filter-heading d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3") {
        div("filter-intro") {
            div("filter-title text-uppercase text-primary small fw-semibold") { +UiMessages.app_filterFeed() }
            p("filter-hint small text-body-secondary mb-0 mt-1") { +UiMessages.app_filterHint() }
        }
        div("filter-actions d-flex flex-wrap align-items-center justify-content-end gap-2") {
            button("filter-clear $secondaryNavigationButtonClass") {
                type("button")
                span("bi bi-x-lg me-1") {}
                +UiMessages.app_clear()
                clicks handledBy { runtime.catalogCommands.clearFilters() }
            }
            button("filter-apply $primaryNavigationButtonClass") {
                type("button")
                span("bi bi-funnel me-1") {}
                +UiMessages.app_applyFilters()
                clicks handledBy { runtime.catalogCommands.applyFilters() }
            }
        }
    }
}
