package f95gm.server.routes.ports

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.Application
import io.ktor.util.AttributeKey

private val applicationDependenciesKey = AttributeKey<ApplicationRouteDependencies>("f95gm.applicationDependencies")

internal fun Application.installApplicationDependencies(dependencies: ApplicationRouteDependencies) {
    attributes.put(applicationDependenciesKey, dependencies)
}

internal val ApplicationCall.applicationDependencies: ApplicationRouteDependencies
    get() = application.attributes[applicationDependenciesKey]
