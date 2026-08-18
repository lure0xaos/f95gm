package f95gm.client.ui.pages

import dev.fritz2.core.RenderContext
import f95gm.client.routing.navigation.Route
import f95gm.client.routing.pages.Pages
import f95gm.client.routing.pages.routeFilterId
import f95gm.client.routing.pages.routePage
import f95gm.client.routing.pages.routeThreadId
import f95gm.client.ui.pages.latestupdates.latestUpdatesDetailPage
import f95gm.client.ui.pages.latestupdates.latestUpdatesPage
import f95gm.client.ui.pages.mygames.myGamesDetailPage
import f95gm.client.ui.pages.mygames.myGamesPage
import f95gm.client.ui.pages.settings.settingsPage

internal fun RenderContext.authenticatedPage(route: Route) {
    when (route[routePage]) {
        Pages.MY_GAMES -> myGamesPage()
        Pages.SETTINGS -> settingsPage()
        Pages.LATEST_UPDATES_DETAIL, Pages.MY_GAMES_DETAIL -> route[routeThreadId]
            ?.takeIf { it.toLongOrNull()?.let { id -> id > 0 } == true }
            ?.let { threadId ->
                if (route[routePage] == Pages.MY_GAMES_DETAIL) myGamesDetailPage(threadId)
                else latestUpdatesDetailPage(threadId)
            }
            ?: latestUpdatesPage()

        else -> latestUpdatesPage(route[routeFilterId])
    }
}
