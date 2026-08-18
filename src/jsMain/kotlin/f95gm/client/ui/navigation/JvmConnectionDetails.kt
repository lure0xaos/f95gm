package f95gm.client.ui.navigation

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.state.runtime.jvmConnection
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.map

internal fun RenderContext.jvmConnectionDetails() {
    div("connection-diagnostics dropdown") {
        button("connection-diagnostics-toggle $secondaryNavigationButtonClass btn-sm px-2 dropdown-toggle") {
            type("button")
            attr("data-bs-toggle", "dropdown")
            attr("data-bs-auto-close", "true")
            attr("aria-expanded", "false")
            attr("title", UiMessages.app_connectionDiagnostics())
            attr("aria-label", UiMessages.app_connectionDiagnostics())
            span("jvm-connection-indicator rounded-circle d-inline-block me-1") {
                className(jvmConnection.data.map { state ->
                    "jvm-connection-indicator rounded-circle d-inline-block me-1 " +
                            if (state.connected) "bg-success" else "bg-danger"
                })
                attr("style", "width: 0.65rem; height: 0.65rem;")
            }
            span("d-none d-sm-inline") {
                jvmConnection.data.map { jvmConnectionStatusLabel(it.status) }.render { +it }
            }
        }
        div("connection-diagnostics-menu dropdown-menu dropdown-menu-end p-3 shadow") {
            div("d-flex align-items-start justify-content-between gap-3 mb-3") {
                h2("h6 mb-0") { +UiMessages.app_connectionDiagnostics() }
                button("btn-close") {
                    type("button")
                    attr("aria-label", UiMessages.app_close())
                    attr("title", UiMessages.app_close())
                }
            }
            jvmConnection.data.render { state ->
                div("small") {
                    div("fw-semibold") { +UiMessages.app_localJvmServer() }
                    div("text-body-secondary") { +jvmConnectionStatusLabel(state.status) }
                    div("text-body-secondary") {
                        +(state.healthStatus?.let { UiMessages.app_httpStatus(it.toString()) }
                            ?: UiMessages.app_noRequestYet())
                    }
                    div("fw-semibold mt-3") { +UiMessages.app_f95zone() }
                    div("text-body-secondary") { +upstreamConnectionStatusLabel(state.upstreamStatus) }
                    div("text-body-secondary") {
                        +(state.upstreamHttpStatus?.let { UiMessages.app_httpStatus(it.toString()) }
                            ?: UiMessages.app_noRequestYet())
                    }
                    if (state.upstreamPath.isNotBlank()) {
                        div("text-body-secondary text-break") { +state.upstreamPath }
                    }
                }
            }
        }
    }
}
