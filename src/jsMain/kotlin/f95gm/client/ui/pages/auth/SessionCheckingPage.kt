package f95gm.client.ui.pages.auth

import dev.fritz2.core.RenderContext
import f95gm.messages.UiMessages

internal fun RenderContext.sessionCheckingPage() {
    div("session-page container min-vh-100 d-flex align-items-center") {
        div("session-content py-5") {
            div("session-kicker text-uppercase text-primary small fw-semibold") { +UiMessages.app_catalogClient() }
            h1("session-title display-5 fw-bold mt-3") { +UiMessages.app_restoringSession() }
            p("session-hint lead text-body-secondary") { +UiMessages.app_checkingSession() }
        }
    }
}
