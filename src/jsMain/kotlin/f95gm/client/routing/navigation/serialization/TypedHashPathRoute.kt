package f95gm.client.routing.navigation.serialization

import f95gm.client.routing.navigation.RouteAdapter
import f95gm.client.routing.navigation.TypedRoute
import dev.fritz2.routing.Route as FritzRoute

internal class TypedHashPathRoute(override val default: TypedRoute) : FritzRoute<TypedRoute> {
    override fun deserialize(hash: String): TypedRoute = RouteAdapter.decode(RouteLocation.fromHash(hash))

    override fun serialize(route: TypedRoute): String = RouteLocation.toHash(RouteAdapter.encode(route))
}
