package f95gm.server.state.runtime

import io.ktor.server.application.*
import io.ktor.util.*

internal val routeDependenciesKey = AttributeKey<RouteDependencies>("f95gm.routeDependencies")

internal fun Application.installServerContext(context: ServerContext) {
    attributes.put(routeDependenciesKey, context)
}

internal val ApplicationCall.routeDependencies: RouteDependencies
    get() = application.attributes[routeDependenciesKey]
