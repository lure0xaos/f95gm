package f95gm.server.routes.filters

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey

private val filterDependenciesKey = AttributeKey<FilterRouteDependencies>("f95gm.filterDependencies")

internal fun Application.installFilterDependencies(dependencies: FilterRouteDependencies) {
    attributes.put(filterDependenciesKey, dependencies)
}

internal val ApplicationCall.filterDependencies: FilterRouteDependencies
    get() = application.attributes[filterDependenciesKey]
