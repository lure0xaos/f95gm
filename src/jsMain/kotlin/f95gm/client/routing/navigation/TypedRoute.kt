package f95gm.client.routing.navigation

import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

internal sealed interface TypedRoute {
    data object MyGames : TypedRoute
    data object Settings : TypedRoute
    data object Login : TypedRoute
    data class LatestUpdates(val filterId: F95FilterId? = null) : TypedRoute
    data class LatestUpdatesDetail(val threadId: F95ThreadId) : TypedRoute
    data class MyGamesDetail(val threadId: F95ThreadId) : TypedRoute
}
