package f95gm.client.routing.navigation

import f95gm.domain.values.F95FilterId

internal fun filterRoute(filterId: F95FilterId): TypedRoute = TypedRoute.LatestUpdates(filterId)
