package f95gm.client.routing.navigation

import f95gm.client.routing.navigation.serialization.Route
import f95gm.client.routing.navigation.serialization.filterId
import f95gm.client.routing.navigation.serialization.threadId
import f95gm.client.routing.pages.Pages
import f95gm.client.routing.pages.routeFilterId
import f95gm.client.routing.pages.routePage
import f95gm.client.routing.pages.routeThreadId

/** The only boundary where the Fritz2 string-map route representation is used. */
internal object RouteAdapter {
    fun decode(route: Route): TypedRoute = when (route[routePage]) {
        Pages.MY_GAMES -> route.threadId()?.let(TypedRoute::MyGamesDetail) ?: TypedRoute.MyGames
        Pages.SETTINGS -> TypedRoute.Settings
        Pages.LOGIN -> TypedRoute.Login
        Pages.LATEST_UPDATES_DETAIL -> route.threadId()?.let(TypedRoute::LatestUpdatesDetail)
            ?: TypedRoute.LatestUpdates()

        Pages.MY_GAMES_DETAIL -> route.threadId()?.let(TypedRoute::MyGamesDetail) ?: TypedRoute.MyGames
        Pages.LATEST_UPDATES -> TypedRoute.LatestUpdates(route.filterId())
        else -> TypedRoute.MyGames
    }

    fun encode(route: TypedRoute): Route = when (route) {
        TypedRoute.MyGames -> mapOf(routePage to Pages.MY_GAMES)
        TypedRoute.Settings -> mapOf(routePage to Pages.SETTINGS)
        TypedRoute.Login -> mapOf(routePage to Pages.LOGIN)
        is TypedRoute.LatestUpdates -> buildMap {
            put(routePage, Pages.LATEST_UPDATES)
            route.filterId?.let { put(routeFilterId, it.value) }
        }

        is TypedRoute.LatestUpdatesDetail -> mapOf(
            routePage to Pages.LATEST_UPDATES_DETAIL,
            routeThreadId to route.threadId.value
        )

        is TypedRoute.MyGamesDetail -> mapOf(routePage to Pages.MY_GAMES_DETAIL, routeThreadId to route.threadId.value)
    }
}
