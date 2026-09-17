package f95gm.server.routes.marks

import f95gm.server.routes.marks.live.MarkRouteDependencies

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey

private val markDependenciesKey = AttributeKey<MarkRouteDependencies>("f95gm.markDependencies")

internal fun Application.installMarkDependencies(dependencies: MarkRouteDependencies) {
    attributes.put(markDependenciesKey, dependencies)
}

internal val ApplicationCall.markDependencies: MarkRouteDependencies
    get() = application.attributes[markDependenciesKey]
