package f95gm.client.routing.navigation

import f95gm.client.routing.navigation.serialization.RouteLocation
import f95gm.client.state.runtime.BrowserEffects

internal fun initialRoute(browser: BrowserEffects): TypedRoute =
    RouteLocation.fromPathTyped(browser.pathname, browser.search)
