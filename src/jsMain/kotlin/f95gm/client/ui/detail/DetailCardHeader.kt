package f95gm.client.ui.detail

import dev.fritz2.core.RenderContext

internal fun RenderContext.detailCardHeader(title: String, bodyId: String, iconClass: String? = null) {
    div("detail-header card-header text-uppercase text-primary small fw-semibold d-flex align-items-center justify-content-between gap-2") {
        attr("role", "button")
        attr("tabindex", "0")
        attr("data-bs-toggle", "collapse")
        attr("data-bs-target", "#$bodyId")
        attr("aria-controls", bodyId)
        attr("aria-expanded", "false")
        attr("style", "cursor: pointer;")
        span("header-title d-flex align-items-center gap-1") {
            if (iconClass != null) span(iconClass) {}
            +title
        }
        span("header-icon bi bi-chevron-down text-body-secondary") {}
    }
}
