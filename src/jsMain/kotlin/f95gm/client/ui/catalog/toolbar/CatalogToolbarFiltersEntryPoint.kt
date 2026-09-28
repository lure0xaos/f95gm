package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.RenderContext
import f95gm.client.state.runtime.ClientCatalogUiPort

/** Stable toolbar entry point; the detailed filter panel is isolated in [catalogFilterPanel]. */
internal fun RenderContext.catalogToolbarFilters(runtime: ClientCatalogUiPort) {
    catalogFilterPanel(runtime)
}
