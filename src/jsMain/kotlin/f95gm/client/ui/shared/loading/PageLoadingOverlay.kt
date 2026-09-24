package f95gm.client.ui.shared.loading

import dev.fritz2.core.RenderContext

internal fun RenderContext.pageLoadingOverlay(message: String) {
    div("page-loading-overlay position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center") {
        attr("role", "status")
        attr("aria-live", "polite")
        div("d-flex align-items-center gap-3 text-body-emphasis") {
            span("spinner-border text-primary") {
                attr("aria-hidden", "true")
            }
            span { +message }
        }
    }
}
