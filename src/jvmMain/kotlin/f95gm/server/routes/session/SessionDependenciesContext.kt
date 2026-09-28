package f95gm.server.routes.session

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey

private val loginDependenciesKey = AttributeKey<LoginRouteDependencies>("f95gm.loginDependencies")
private val sessionDependenciesKey = AttributeKey<SessionRouteDependencies>("f95gm.sessionDependencies")

internal fun Application.installLoginDependencies(dependencies: LoginRouteDependencies) {
    attributes.put(loginDependenciesKey, dependencies)
}

internal fun Application.installSessionDependencies(dependencies: SessionRouteDependencies) {
    attributes.put(sessionDependenciesKey, dependencies)
}

internal val ApplicationCall.loginDependencies: LoginRouteDependencies
    get() = application.attributes[loginDependenciesKey]

internal val ApplicationCall.sessionDependencies: SessionRouteDependencies
    get() = application.attributes[sessionDependenciesKey]
