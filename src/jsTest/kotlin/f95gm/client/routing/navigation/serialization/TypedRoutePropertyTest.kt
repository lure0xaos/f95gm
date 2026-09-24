package f95gm.client.routing.navigation.serialization

import f95gm.client.routing.navigation.RouteAdapter
import f95gm.client.routing.navigation.TypedRoute
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import kotlin.test.Test
import kotlin.test.assertEquals

class TypedRoutePropertyTest {
    @Test
    fun supportedTypedRoutesRoundTripAcrossTheUrlBoundary() {
        val routes = buildList {
            add(TypedRoute.MyGames)
            add(TypedRoute.Settings)
            add(TypedRoute.Login)
            add(TypedRoute.LatestUpdates())
            add(TypedRoute.LatestUpdates(F95FilterId("filter-1")))
            add(TypedRoute.LatestUpdatesDetail(F95ThreadId("123")))
            add(TypedRoute.MyGamesDetail(F95ThreadId("456")))
        }
        routes.forEach { route ->
            assertEquals(
                route,
                RouteAdapter.decode(RouteLocation.fromHash(RouteLocation.toHash(RouteAdapter.encode(route))))
            )
        }
    }
}
