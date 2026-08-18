package f95gm.client.routing.navigation

import f95gm.client.routing.pages.Pages
import f95gm.client.routing.pages.routePage
import f95gm.client.routing.pages.routeThreadId

internal fun latestUpdatesDetailRoute(threadId: String): Route = mapOf(
    routePage to Pages.LATEST_UPDATES_DETAIL,
    routeThreadId to threadId
)
