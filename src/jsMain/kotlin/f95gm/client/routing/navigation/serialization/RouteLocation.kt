package f95gm.client.routing.navigation.serialization

import dev.fritz2.routing.decodeURIComponent
import dev.fritz2.routing.encodeURIComponent
import f95gm.client.routing.navigation.Route
import f95gm.client.routing.pages.*

internal object RouteLocation {
    fun fromPath(pathname: String, search: String): Route {
        val threadId = itemThreadId(pathname)
        val query = parameters(search)
        val baseRoute = threadId?.let {
            mapOf(routePage to Pages.LATEST_UPDATES_DETAIL, routeThreadId to it)
        } ?: mapOf(routePage to (query[routePage] ?: Pages.MY_GAMES))
        return baseRoute + query.filterKeys { it != routePage && it != routeThreadId }
    }

    fun fromHash(hash: String): Route {
        val serialized = hash.removePrefix("#")
        val queryStart = serialized.indexOf('?')
        val path = if (queryStart >= 0) serialized.substring(0, queryStart) else serialized
        val query = parameters(queryStart.takeIf { it >= 0 }?.let { serialized.substring(it + 1) }.orEmpty())
        val pathRoute = pathRoute(path)
        if (pathRoute != null) return pathRoute + query.filterKeys { it != routePage && it != routeThreadId }

        val legacyRoute = parameters(serialized)
        val page = legacyRoute[routePage] ?: query[routePage] ?: Pages.MY_GAMES
        return (legacyRoute.takeIf { it.containsKey(routePage) } ?: mapOf(routePage to page)) +
                query.filterKeys { it != routePage && it != routeThreadId }
    }

    fun toHash(route: Route): String {
        val page = route[routePage]
        val path = when (page) {
            Pages.LATEST_UPDATES_DETAIL -> detailPath(Pages.LATEST_UPDATES, route[routeThreadId])
            Pages.MY_GAMES_DETAIL -> detailPath(Pages.MY_GAMES, route[routeThreadId])
            Pages.LATEST_UPDATES -> filterPath(route[routeFilterId])
            Pages.LOGIN, Pages.MY_GAMES, Pages.SETTINGS -> "$page/"
            else -> "${Pages.MY_GAMES}/"
        }
        val query = route.entries
            .filter { (key, _) -> key != routePage && key != routeThreadId && key != routeFilterId }
            .joinToString("&") { (key, value) -> "$key=${encodeURIComponent(value)}" }
        return path + query.takeIf { it.isNotEmpty() }?.let { "?$it" }.orEmpty()
    }

    private fun pathRoute(path: String): Route? {
        val segments = path.split('/').filter { it.isNotBlank() }
        val page = segments.firstOrNull() ?: return null
        val threadId = segments.getOrNull(1)?.takeIf(::isThreadId)
        return when {
            page == Pages.LATEST_UPDATES && segments.size == 1 -> mapOf(routePage to Pages.LATEST_UPDATES)
            page == Pages.LATEST_UPDATES && segments.size == 2 && threadId != null -> mapOf(
                routePage to Pages.LATEST_UPDATES_DETAIL,
                routeThreadId to threadId
            )

            page == Pages.LATEST_UPDATES && segments.size == 2 -> mapOf(
                routePage to Pages.LATEST_UPDATES,
                routeFilterId to decodeURIComponent(segments[1])
            )

            page == Pages.MY_GAMES && segments.size == 1 -> mapOf(routePage to Pages.MY_GAMES)
            page == Pages.MY_GAMES && segments.size == 2 && threadId != null -> mapOf(
                routePage to Pages.MY_GAMES_DETAIL,
                routeThreadId to threadId
            )

            page == Pages.LOGIN && segments.size == 1 -> mapOf(routePage to Pages.LOGIN)
            page == Pages.SETTINGS && segments.size == 1 -> mapOf(routePage to Pages.SETTINGS)
            page == "item" && segments.size == 2 && threadId != null -> mapOf(
                routePage to Pages.LATEST_UPDATES_DETAIL,
                routeThreadId to threadId
            )

            else -> null
        }
    }

    private fun detailPath(page: String, threadId: String?): String =
        threadId?.let { "$page/$it/" } ?: "$page/"

    private fun filterPath(filterId: String?): String =
        filterId?.let { "${Pages.LATEST_UPDATES}/${encodeURIComponent(it)}/" }
            ?: "${Pages.LATEST_UPDATES}/"

    private fun parameters(serialized: String): Map<String, String> = serialized
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
        .toMap()

    private fun isThreadId(value: String): Boolean =
        value.isNotEmpty() && value.all { character -> character in '0'..'9' }
}
