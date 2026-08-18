package f95gm.client.routing.navigation

import f95gm.client.routing.pages.Pages
import f95gm.client.routing.pages.routePage
import f95gm.client.routing.pages.routeThreadId

internal fun myGamesDetailRoute(threadId: String): Route = mapOf(
    routePage to Pages.MY_GAMES_DETAIL,
    routeThreadId to threadId
)
