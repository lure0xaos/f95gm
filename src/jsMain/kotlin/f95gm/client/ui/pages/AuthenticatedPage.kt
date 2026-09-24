package f95gm.client.ui.pages

import dev.fritz2.core.RenderContext
import f95gm.client.routing.navigation.TypedRoute
import f95gm.client.ui.pages.latestupdates.latestUpdatesDetailPage
import f95gm.client.ui.pages.latestupdates.latestUpdatesPage
import f95gm.client.ui.pages.mygames.myGamesDetailPage
import f95gm.client.ui.pages.mygames.myGamesPage
import f95gm.client.ui.pages.settings.settingsPage

internal fun RenderContext.authenticatedPage(typed: TypedRoute) {
    when (typed) {
        TypedRoute.MyGames -> myGamesPage()
        TypedRoute.Settings -> settingsPage()
        TypedRoute.Login -> latestUpdatesPage()
        is TypedRoute.LatestUpdates -> latestUpdatesPage(typed.filterId?.value)
        is TypedRoute.LatestUpdatesDetail -> latestUpdatesDetailPage(typed.threadId.value)
        is TypedRoute.MyGamesDetail -> myGamesDetailPage(typed.threadId.value)
    }
}
