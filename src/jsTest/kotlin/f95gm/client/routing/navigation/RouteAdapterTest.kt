package f95gm.client.routing.navigation

import f95gm.client.routing.pages.Pages
import f95gm.client.routing.pages.routeFilterId
import f95gm.client.routing.pages.routePage
import f95gm.client.routing.pages.routeThreadId
import f95gm.client.routing.navigation.serialization.RouteParameters
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteAdapterTest {
    @Test
    fun allTypedRoutesRoundTripThroughRouteParameters() {
        listOf(
            TypedRoute.MyGames,
            TypedRoute.Settings,
            TypedRoute.Login,
            TypedRoute.LatestUpdates(),
            TypedRoute.LatestUpdates(F95FilterId("favorites")),
            TypedRoute.LatestUpdatesDetail(F95ThreadId("123")),
            TypedRoute.MyGamesDetail(F95ThreadId("456"))
        ).forEach { route ->
            assertEquals(route, RouteAdapter.decode(RouteAdapter.encode(route)), route.toString())
        }
    }

    @Test
    fun supportsTypedRoutesAndSafeFallbacks() {
        assertEquals(
            TypedRoute.MyGamesDetail(F95ThreadId("123")),
            RouteAdapter.decode(RouteParameters(mapOf(routePage to Pages.MY_GAMES, routeThreadId to "123")))
        )
        assertEquals(
            TypedRoute.MyGames,
            RouteAdapter.decode(RouteParameters(mapOf(routePage to Pages.MY_GAMES_DETAIL)))
        )
        assertEquals(
            TypedRoute.LatestUpdates(),
            RouteAdapter.decode(RouteParameters(mapOf(routePage to Pages.LATEST_UPDATES_DETAIL)))
        )
        assertEquals(
            TypedRoute.MyGames,
            RouteAdapter.decode(RouteParameters(mapOf(routePage to "unknown", routeFilterId to "ignored")))
        )
    }
}
