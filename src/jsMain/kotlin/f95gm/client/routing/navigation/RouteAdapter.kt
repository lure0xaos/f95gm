package f95gm.client.routing.navigation

import f95gm.client.routing.navigation.serialization.RouteParameters
import f95gm.client.routing.navigation.serialization.TypedRouteParameters
import f95gm.client.routing.pages.Pages
import f95gm.client.routing.pages.routeFilterId
import f95gm.client.routing.pages.routePage
import f95gm.client.routing.pages.routeThreadId

/** The only boundary where the Fritz2 string-map route representation is used. */
internal object RouteAdapter {
    fun decode(route: RouteParameters): TypedRoute = TypedRouteParameters.from(route).let { typed -> when (typed.page) {
        Pages.MY_GAMES -> typed.threadId?.let(TypedRoute::MyGamesDetail) ?: TypedRoute.MyGames
        Pages.SETTINGS -> TypedRoute.Settings
        Pages.LOGIN -> TypedRoute.Login
        Pages.LATEST_UPDATES_DETAIL -> typed.threadId?.let(TypedRoute::LatestUpdatesDetail)
            ?: TypedRoute.LatestUpdates()

        Pages.MY_GAMES_DETAIL -> typed.threadId?.let(TypedRoute::MyGamesDetail) ?: TypedRoute.MyGames
        Pages.LATEST_UPDATES -> TypedRoute.LatestUpdates(typed.filterId)
        else -> TypedRoute.MyGames
    } }

    fun encode(route: TypedRoute): RouteParameters = when (route) {
        TypedRoute.MyGames -> RouteParameters(mapOf(routePage to Pages.MY_GAMES))
        TypedRoute.Settings -> RouteParameters(mapOf(routePage to Pages.SETTINGS))
        TypedRoute.Login -> RouteParameters(mapOf(routePage to Pages.LOGIN))
        is TypedRoute.LatestUpdates -> RouteParameters(buildMap {
            put(routePage, Pages.LATEST_UPDATES)
            route.filterId?.let { put(routeFilterId, it.value) }
        })

        is TypedRoute.LatestUpdatesDetail -> RouteParameters(mapOf(
            routePage to Pages.LATEST_UPDATES_DETAIL,
            routeThreadId to route.threadId.value
        ))

        is TypedRoute.MyGamesDetail -> RouteParameters(mapOf(routePage to Pages.MY_GAMES_DETAIL, routeThreadId to route.threadId.value))
    }
}
