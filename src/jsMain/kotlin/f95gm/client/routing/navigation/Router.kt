package f95gm.client.routing.navigation

import dev.fritz2.routing.Router
import dev.fritz2.routing.routerOf
import f95gm.client.routing.navigation.serialization.TypedHashPathRoute
import f95gm.client.state.runtime.WindowBrowserEffects
import kotlinx.coroutines.Job

internal val router: Router<TypedRoute> = routerOf(TypedHashPathRoute(initialRoute(WindowBrowserEffects)), Job())
