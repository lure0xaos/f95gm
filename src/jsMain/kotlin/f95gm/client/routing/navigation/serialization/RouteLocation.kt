package f95gm.client.routing.navigation.serialization

import dev.fritz2.routing.decodeURIComponent
import dev.fritz2.routing.encodeURIComponent
import f95gm.client.routing.navigation.RouteAdapter
import f95gm.client.routing.navigation.TypedRoute
import f95gm.client.routing.pages.*
import f95gm.domain.values.F95ThreadId

internal object RouteLocation {
    fun fromPathTyped(pathname: String, search: String): TypedRoute =
        RouteAdapter.decode(fromPath(pathname, search))

    fun fromHashTyped(hash: String): TypedRoute = RouteAdapter.decode(fromHash(hash))

    fun toHash(route: TypedRoute): String = toHash(RouteAdapter.encode(route))

    fun fromPath(pathname: String, search: String): RouteParameters {
        val threadId = itemThreadId(pathname)
        val query = parameters(search)
        val baseRoute = threadId?.let {
            mapOf(routePage to Pages.LATEST_UPDATES_DETAIL, routeThreadId to it.value)
        } ?: mapOf(routePage to (query[RouteParameterKey.PAGE] ?: Pages.MY_GAMES))
        return RouteParameters(baseRoute + query.values.filterKeys { it != routePage && it != routeThreadId })
    }

    fun fromHash(hash: String): RouteParameters {
        val serialized = hash.removePrefix("#")
        val queryStart = serialized.indexOf('?')
        val path = if (queryStart >= 0) serialized.substring(0, queryStart) else serialized
        val query = parameters(queryStart.takeIf { it >= 0 }?.let { serialized.substring(it + 1) }.orEmpty())
        val pathRoute = pathRoute(path)
        if (pathRoute != null) return pathRoute + query.values.filterKeys { it != routePage && it != routeThreadId }

        return RouteParameters(mapOf(routePage to Pages.MY_GAMES) + query.values.filterKeys { it != routePage && it != routeThreadId })
    }

    fun toHash(route: RouteParameters): String {
        val page = route[RouteParameterKey.PAGE]
        val path = when (page) {
            Pages.LATEST_UPDATES_DETAIL -> detailPath(Pages.LATEST_UPDATES, route.threadId())
            Pages.MY_GAMES_DETAIL -> detailPath(Pages.MY_GAMES, route.threadId())
            Pages.LATEST_UPDATES -> filterPath(route.filterId())
            Pages.LOGIN, Pages.MY_GAMES, Pages.SETTINGS -> "$page/"
            else -> "${Pages.MY_GAMES}/"
        }
        val query = route.entries
            .filter { (key, _) -> key != routePage && key != routeThreadId && key != routeFilterId }
            .joinToString("&") { (key, value) -> "$key=${encodeURIComponent(value)}" }
        return path + query.takeIf { it.isNotEmpty() }?.let { "?$it" }.orEmpty()
    }

    @Suppress("CyclomaticComplexMethod")
    private fun pathRoute(path: String): RouteParameters? {
        val segments = path.split('/').filter { it.isNotBlank() }
        val page = segments.firstOrNull() ?: return null
        val threadId = segments.getOrNull(1)?.takeIf(::isThreadId)
            ?.let(::F95ThreadId)
        return when {
            page == Pages.LATEST_UPDATES && segments.size == 1 -> RouteParameters(mapOf(routePage to Pages.LATEST_UPDATES))
            page == Pages.LATEST_UPDATES && segments.size == 2 && threadId != null -> RouteParameters(mapOf(
                routePage to Pages.LATEST_UPDATES_DETAIL,
                routeThreadId to threadId.value
            ))

            page == Pages.LATEST_UPDATES && segments.size == 2 -> RouteParameters(mapOf(
                routePage to Pages.LATEST_UPDATES,
                routeFilterId to decodeURIComponent(segments[1])
            ))

            page == Pages.MY_GAMES && segments.size == 1 -> RouteParameters(mapOf(routePage to Pages.MY_GAMES))
            page == Pages.MY_GAMES && segments.size == 2 && threadId != null -> RouteParameters(mapOf(
                routePage to Pages.MY_GAMES_DETAIL,
                routeThreadId to threadId.value
            ))

            page == Pages.LOGIN && segments.size == 1 -> RouteParameters(mapOf(routePage to Pages.LOGIN))
            page == Pages.SETTINGS && segments.size == 1 -> RouteParameters(mapOf(routePage to Pages.SETTINGS))
            page == "item" && segments.size == 2 && threadId != null -> RouteParameters(mapOf(
                routePage to Pages.LATEST_UPDATES_DETAIL,
                routeThreadId to threadId.value
            ))

            else -> null
        }
    }

    private fun detailPath(page: String, threadId: f95gm.domain.values.F95ThreadId?): String =
        threadId?.let { "$page/${it.value}/" } ?: "$page/"

    private fun filterPath(filterId: f95gm.domain.values.F95FilterId?): String =
        filterId?.let { "${Pages.LATEST_UPDATES}/${encodeURIComponent(it.value)}/" }
            ?: "${Pages.LATEST_UPDATES}/"

    private fun parameters(serialized: String): RouteParameters = RouteParameters(serialized
        .removePrefix("?")
        .removePrefix("#")
        .split('&')
        .filter { it.isNotBlank() }
        .mapNotNull { parameter ->
            val separator = parameter.indexOf('=')
            if (separator <= 0) null
            else decodeURIComponent(parameter.substring(0, separator)) to
                    decodeURIComponent(parameter.substring(separator + 1))
        }
        .toMap())

    private fun isThreadId(value: String): Boolean =
        value.isNotEmpty() && value.all { character -> character in '0'..'9' }
}
