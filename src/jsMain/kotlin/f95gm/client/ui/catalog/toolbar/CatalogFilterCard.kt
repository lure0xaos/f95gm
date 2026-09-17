package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*

internal fun RenderContext.catalogFilterCard(title: String, content: RenderContext.() -> Unit) {
    div("col d-flex") {
        div("filter-card card p-3 w-100 h-100 d-flex flex-column") {
            div("filter-title text-uppercase text-primary small fw-semibold mb-3") { +title }
            div("row row-cols-1 g-3 align-content-start flex-grow-1") { content() }
        }
    }
}
