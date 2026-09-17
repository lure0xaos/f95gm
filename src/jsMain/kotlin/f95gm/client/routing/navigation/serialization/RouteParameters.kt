package f95gm.client.routing.navigation.serialization

import f95gm.client.routing.pages.routeFilterId
import f95gm.client.routing.pages.routePage
import f95gm.client.routing.pages.routeThreadId
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

/** Explicit raw routing boundary; feature code consumes [TypedRouteParameters]. */
internal value class RouteParameters(val values: Map<String, String>) {
    operator fun get(key: String): String? = values[key]
    val entries: Set<Map.Entry<String, String>> get() = values.entries
    operator fun plus(other: Map<String, String>): RouteParameters = RouteParameters(values + other)
}

internal enum class RouteParameterKey(val raw: String) {
    PAGE(routePage), THREAD_ID(routeThreadId), FILTER_ID(routeFilterId)
}

internal operator fun RouteParameters.get(key: RouteParameterKey): String? = this[key.raw]

internal fun RouteParameters.threadId(): F95ThreadId? = this[RouteParameterKey.THREAD_ID]?.let(::F95ThreadId)
internal fun RouteParameters.filterId(): F95FilterId? = this[RouteParameterKey.FILTER_ID]?.let(::F95FilterId)

/** Typed representation used after the Fritz2 map has crossed the routing boundary. */
internal data class TypedRouteParameters(
    val page: String?,
    val threadId: F95ThreadId?,
    val filterId: F95FilterId?
) {
    companion object {
        fun from(route: RouteParameters) = TypedRouteParameters(route[RouteParameterKey.PAGE], route.threadId(), route.filterId())
    }

    fun asRouteParameters(): RouteParameters = RouteParameters(buildMap {
        page?.let { put(routePage, it) }
        threadId?.let { put(routeThreadId, it.value) }
        filterId?.let { put(routeFilterId, it.value) }
    })
}
