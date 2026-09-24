package f95gm.client.routing.navigation

import f95gm.domain.values.F95ThreadId

internal fun latestUpdatesDetailRoute(threadId: F95ThreadId): TypedRoute = TypedRoute.LatestUpdatesDetail(threadId)
