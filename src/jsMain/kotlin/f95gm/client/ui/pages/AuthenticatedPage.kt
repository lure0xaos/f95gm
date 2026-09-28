package f95gm.client.ui.pages

import dev.fritz2.core.RenderContext
import f95gm.client.routing.navigation.TypedRoute
import f95gm.client.state.runtime.ClientUiPort
import f95gm.client.ui.pages.latestupdates.latestUpdatesDetailPage
import f95gm.client.ui.pages.latestupdates.latestUpdatesPage
import f95gm.client.ui.pages.mygames.myGamesDetailPage
import f95gm.client.ui.pages.mygames.myGamesPage
import f95gm.client.ui.pages.settings.settingsPage

internal fun RenderContext.authenticatedPage(runtime: ClientUiPort, typed: TypedRoute) {
    when (typed) {
        TypedRoute.MyGames -> myGamesPage(runtime)
        TypedRoute.Settings -> settingsPage(runtime)
        TypedRoute.Login -> latestUpdatesPage(runtime)
        is TypedRoute.LatestUpdates -> latestUpdatesPage(runtime, typed.filterId?.value)
        is TypedRoute.LatestUpdatesDetail -> latestUpdatesDetailPage(runtime, typed.threadId)
        is TypedRoute.MyGamesDetail -> myGamesDetailPage(runtime, typed.threadId)
    }
}
