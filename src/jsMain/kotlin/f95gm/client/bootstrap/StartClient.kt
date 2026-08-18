package f95gm.client.bootstrap

import dev.fritz2.core.Window
import dev.fritz2.core.addGlobalStyles
import dev.fritz2.core.render
import dev.fritz2.core.type
import f95gm.client.actions.auth.restoreSession
import f95gm.client.actions.runtime.retryJvmConnection
import f95gm.client.actions.runtime.startJvmConnectionMonitor
import f95gm.client.model.feedback.ToastState
import f95gm.client.routing.navigation.router
import f95gm.client.state.feedback.toast
import f95gm.client.state.runtime.BrowserTabId
import f95gm.client.state.runtime.jvmConnection
import f95gm.client.state.runtime.scope
import f95gm.client.state.session.session
import f95gm.client.state.settings.appSettings
import f95gm.client.ui.detail.gallery.detailGallery
import f95gm.client.ui.pages.auth.loginPage
import f95gm.client.ui.pages.auth.sessionCheckingPage
import f95gm.client.ui.pages.authenticatedPage
import f95gm.client.ui.styles.applicationStyles
import f95gm.domain.api.F95Api
import f95gm.messages.UiMessages
import kotlinx.browser.document
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal fun startClient() {
    loadBootstrapAssets()
    addGlobalStyles(applicationStyles)
    startJvmConnectionMonitor()
    document.title = UiMessages.app_pageTitle()
    render {
        Window.pagehides handledBy {
            navigator.sendBeacon("${F95Api.SHUTDOWN}?${F95Api.TAB_ID}=${BrowserTabId.value}").run { }
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
                                clicks handledBy { retryJvmConnection() }
                            }
                        }
                    }
                }
                div("view-root") {
                    session.data.map { it.checking to it.authenticated }.distinctUntilChanged().render {
                        if (it.first) sessionCheckingPage()
                        else if (!it.second) loginPage()
                        else router.data.render { route -> authenticatedPage(route) }
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
            detailGallery()
        }
    }
    scope.launch { restoreSession() }
}
