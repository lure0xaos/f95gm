package f95gm.client.ui.catalog.filters

import dev.fritz2.core.RenderContext

internal fun RenderContext.filterField(
    fieldLabel: String,
    content: RenderContext.() -> Unit
) {
    div("filter-field d-flex flex-nowrap align-items-center gap-2 w-100") {
        label("field-label form-label small text-uppercase text-body-secondary fw-semibold text-nowrap flex-shrink-0 mb-0") {
            +fieldLabel
        }
        div("field-control flex-grow-1 flex-shrink-1 overflow-visible") {
            content()
        }
    }
}
