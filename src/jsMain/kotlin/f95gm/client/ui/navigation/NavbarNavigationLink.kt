package f95gm.client.ui.navigation

import dev.fritz2.core.RenderContext
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.client.routing.navigation.myGamesRoute
import f95gm.client.routing.navigation.router
import f95gm.client.ui.shared.navigation.navigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.map

internal fun RenderContext.navbarNavigationLink(catalogLink: Boolean) {
    a("page-nav $navigationButtonClass") {
        clicks.map { if (catalogLink) latestUpdatesRoute else myGamesRoute } handledBy router.navTo
        span(if (catalogLink) "bi bi-grid me-1" else "bi bi-bookmark-heart me-1") {}
        +(if (catalogLink) UiMessages.app_catalog() else UiMessages.app_myGames())
    }
}
