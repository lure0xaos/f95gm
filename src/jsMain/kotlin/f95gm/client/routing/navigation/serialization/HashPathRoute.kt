package f95gm.client.routing.navigation.serialization

import f95gm.client.routing.navigation.Route
import dev.fritz2.routing.Route as FritzRoute

internal class HashPathRoute(override val default: Route) : FritzRoute<Route> {
    override fun deserialize(hash: String): Route = RouteLocation.fromHash(hash)

    override fun serialize(route: Route): String = RouteLocation.toHash(route)
}
