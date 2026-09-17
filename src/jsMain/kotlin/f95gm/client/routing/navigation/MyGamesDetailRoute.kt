package f95gm.client.routing.navigation

import f95gm.domain.values.F95ThreadId

internal fun myGamesDetailRoute(threadId: F95ThreadId): TypedRoute = TypedRoute.MyGamesDetail(threadId)
