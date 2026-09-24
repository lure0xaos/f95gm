package f95gm.client.routing.navigation

import f95gm.client.routing.navigation.serialization.RouteLocation
import f95gm.client.state.runtime.BrowserRuntime

internal fun initialRoute(): TypedRoute =
    RouteLocation.fromPathTyped(BrowserRuntime.pathname, BrowserRuntime.search)
