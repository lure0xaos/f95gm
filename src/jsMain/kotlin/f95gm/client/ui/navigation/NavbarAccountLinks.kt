package f95gm.client.ui.navigation

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.actions.auth.signOut
import f95gm.client.routing.navigation.router
import f95gm.client.routing.navigation.settingsRoute
import f95gm.client.ui.catalog.saved.savedFilterLinks
import f95gm.client.ui.shared.navigation.dangerNavigationButtonClass
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.map

internal fun RenderContext.navbarAccountLinks(
    leadingAction: RenderContext.() -> Unit = {}
) {
    div("navbar-account account-links d-flex flex-wrap align-items-center gap-2 ms-auto mt-2 mt-xl-0") {
        leadingAction()
        savedFilterLinks()
        a("settings-link $secondaryNavigationButtonClass") {
            clicks.map { settingsRoute } handledBy router.navTo
            span("bi bi-gear me-1") {}
            +UiMessages.app_settings()
        }
        button("sign-out $dangerNavigationButtonClass") {
            type("button")
            span("bi bi-box-arrow-right me-1") {}
            +UiMessages.app_signOut()
            clicks handledBy { signOut() }
        }
    }
}
