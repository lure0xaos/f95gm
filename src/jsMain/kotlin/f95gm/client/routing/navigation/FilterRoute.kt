package f95gm.client.routing.navigation

import f95gm.client.routing.pages.Pages
import f95gm.client.routing.pages.routeFilterId
import f95gm.client.routing.pages.routePage

internal fun filterRoute(filterId: String): Route = mapOf(
    routePage to Pages.LATEST_UPDATES,
    routeFilterId to filterId
)
