package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.state.runtime.ClientCatalogUiPort

internal fun RenderContext.renderCatalogFilterSections(runtime: ClientCatalogUiPort) {
    div("filter-panel collapse w-100 mt-2") {
        id("catalog-filter-panel")
        div("filter-content border-top pt-3") {
            catalogFilterHeader(runtime)
            runtime.catalogOptionsView.data.render { options ->
                div("filter-grid row row-cols-1 row-cols-xl-3 g-3 align-items-stretch") {
                    catalogSearchSection(runtime)
                    catalogMatchingSection(runtime)
                    catalogTagSection(runtime)
                    options.prefixGroups.forEach { group -> catalogPrefixSection(runtime, group) }
                }
            }
            catalogToolbarStatus(runtime)
        }
    }
}
