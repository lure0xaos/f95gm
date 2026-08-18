package f95gm.client.ui.shared.content

import dev.fritz2.core.RenderContext

internal fun RenderContext.feature(icon: String, title: String, copy: String) {
    div("feature-column col") {
        div("feature-card card border-0 shadow-sm h-100 p-3") {
            div("feature-heading d-flex align-items-center gap-2 mb-2") {
                span("feature-icon $icon text-primary fs-5") {}
                h2("feature-title h6 fw-bold mb-0") { +title }
            }
            p("feature-copy small text-body-secondary mb-0") { +copy }
        }
    }
}
