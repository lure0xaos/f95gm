package f95gm.client.bootstrap

import dev.fritz2.core.Window
import dev.fritz2.core.addGlobalStyles
import dev.fritz2.core.render
import dev.fritz2.core.type
import f95gm.client.actions.auth.restoreSession
import f95gm.client.actions.connection.retryJvmConnection
import f95gm.client.actions.connection.startJvmConnectionMonitor
import f95gm.client.model.feedback.ToastState
import f95gm.client.routing.navigation.router
import f95gm.client.state.runtime.*
import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.ApiBrowserTabId
import f95gm.client.ui.detail.gallery.detailGallery
import f95gm.client.ui.pages.auth.loginPage
import f95gm.client.ui.pages.auth.sessionCheckingPage
import f95gm.client.ui.pages.authenticatedPage
import f95gm.client.ui.styles.applicationStyles
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun startClient(state: ClientState) {
    val runtime = ClientComposition.create(state)
    val appSettings = state.shell.appSettings
    val session = state.shell.session
    val toast = state.shell.toast
    val jvmConnection = state.shell.jvmConnection
    runtime.start()
    loadBootstrapAssets()
    addGlobalStyles(applicationStyles)
    startJvmConnectionMonitor(runtime)
    state.browser.setTitle(UiMessages.app_pageTitle())
    render {
        Window.pagehides handledBy {
            state.browser.sendBeacon(F95ApiPaths.shutdownPath(ApiBrowserTabId(runtime.browserTabId.value)).value)
            runtime.close()
        }
        div("app-shell bg-black min-vh-100") {
            attr("data-bs-theme", appSettings.data.map { it.theme.bootstrapValue })
            div("container-fluid px-3 px-md-5 pb-5") {
                jvmConnection.data.map { it.connected }.distinctUntilChanged().render { connected ->
                    if (!connected) {
                        div("alert alert-danger mt-3 mb-0 d-flex align-items-center justify-content-between gap-3") {
                            attr("role", "alert")
                            attr("aria-live", "assertive")
                            span { +UiMessages.app_jvmNotResponding() }
                            button("btn btn-sm btn-outline-danger") {
                                type("button")
                                +UiMessages.app_retry()
                                clicks handledBy { retryJvmConnection(runtime) }
                            }
                        }
                    }
                }
                div("view-root") {
                    session.data.map { it.async.isLoading to it.authenticated }.distinctUntilChanged().render {
                        if (it.first) sessionCheckingPage()
                        else if (!it.second) loginPage(runtime)
                        else router.data.render { route -> authenticatedPage(runtime, route) }
                    }
                }
            }
            toast.data.map { it.message }.distinctUntilChanged().render { message ->
                if (message.isNotBlank()) {
                    div("toast-container position-fixed bottom-0 end-0 p-3") {
                        div("toast show text-bg-warning border-0") {
                            attr("role", "alert")
                            attr("aria-live", "assertive")
                            attr("aria-atomic", "true")
                            div("d-flex") {
                                div("toast-body") { +message }
                                button("btn-close btn-close-white me-2 m-auto") {
                                    type("button")
                                    attr("aria-label", UiMessages.app_close())
                                    clicks handledBy { toast.enqueue { ToastState() } }
                                }
                            }
                        }
                    }
                }
            }
            detailGallery(runtime)
        }
    }
    runtime.launchAuthenticationOperation { restoreSession(runtime) }
}
