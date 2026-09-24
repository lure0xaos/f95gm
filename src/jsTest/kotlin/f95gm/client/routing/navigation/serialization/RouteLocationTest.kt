package f95gm.client.routing.navigation.serialization

import f95gm.client.routing.navigation.RouteAdapter
import f95gm.client.routing.navigation.TypedRoute
import f95gm.client.routing.pages.routeFilterId
import f95gm.client.routing.pages.routePage
import f95gm.client.routing.pages.routeThreadId
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteLocationTest {
    @Test
    fun serializesAndReadsDetailRoutes() {
        val route = mapOf(
            routePage to "latest-updates-detail",
            routeThreadId to "12345"
        )
        assertEquals(route, RouteLocation.fromHash(RouteLocation.toHash(route)))
    }

    @Test
    fun convertsValidDetailRouteToTypedModel() {
        val route = mapOf(routePage to "latest-updates-detail", routeThreadId to "12345")
        assertEquals(TypedRoute.LatestUpdatesDetail(F95ThreadId("12345")), RouteAdapter.decode(route))
    }

    @Test
    fun typedRouteAdapterPreservesFilterRoute() {
        val route = TypedRoute.LatestUpdates(F95FilterId("favorites"))
        assertEquals(route, RouteAdapter.decode(RouteAdapter.encode(route)))
    }

    @Test
    fun encodesAndDecodesUrlEscapedFilterIds() {
        val route = mapOf(routePage to "latest-updates", routeFilterId to "my favorites")

        assertEquals("latest-updates/my%20favorites/", RouteLocation.toHash(route))
        assertEquals(route, RouteLocation.fromHash("#latest-updates/my%20favorites/"))
    }

    @Test
    fun resolvesItemPathsAndFallsBackForMalformedLocations() {
        assertEquals(
            mapOf(routePage to "latest-updates-detail", routeThreadId to "123"),
            RouteLocation.fromPath("/item/123", "")
        )
        assertEquals(
            mapOf(routePage to "settings"),
            RouteLocation.fromHash("#?page=settings")
        )
        assertEquals(
            mapOf(routePage to "my-games"),
            RouteLocation.fromHash("#unknown")
        )
    }
}
