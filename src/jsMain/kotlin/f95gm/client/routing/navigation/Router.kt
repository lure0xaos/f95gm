package f95gm.client.routing.navigation

import dev.fritz2.routing.Router
import dev.fritz2.routing.routerOf
import f95gm.client.routing.navigation.serialization.HashPathRoute
import kotlinx.coroutines.Job

internal val router: Router<Route> = routerOf(HashPathRoute(initialRoute()), Job())
