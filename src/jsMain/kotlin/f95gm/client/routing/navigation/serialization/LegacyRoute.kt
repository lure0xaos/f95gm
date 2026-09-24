package f95gm.client.routing.navigation.serialization

import f95gm.client.routing.pages.routeFilterId
import f95gm.client.routing.pages.routeThreadId
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

/** Fritz2's legacy string-map representation, confined to serialization. */
internal typealias Route = Map<String, String>

internal fun Route.threadId(): F95ThreadId? = this[routeThreadId]?.let(::F95ThreadId)
internal fun Route.filterId(): F95FilterId? = this[routeFilterId]?.let(::F95FilterId)
