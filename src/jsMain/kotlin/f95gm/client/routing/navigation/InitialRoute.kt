package f95gm.client.routing.navigation

import f95gm.client.routing.navigation.serialization.RouteLocation
import kotlinx.browser.window

internal fun initialRoute(): Route =
    RouteLocation.fromPath(window.location.pathname, window.location.search)
