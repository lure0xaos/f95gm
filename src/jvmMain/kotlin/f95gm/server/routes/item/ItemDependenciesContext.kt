package f95gm.server.routes.item

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey

private val itemDependenciesKey = AttributeKey<ItemRouteDependencies>("f95gm.itemDependencies")
private val mediaDependenciesKey = AttributeKey<MediaRouteDependencies>("f95gm.mediaDependencies")

internal fun Application.installItemDependencies(dependencies: ItemRouteDependencies) {
    attributes.put(itemDependenciesKey, dependencies)
}

internal fun Application.installMediaDependencies(dependencies: MediaRouteDependencies) {
    attributes.put(mediaDependenciesKey, dependencies)
}

internal val ApplicationCall.itemDependencies: ItemRouteDependencies
    get() = application.attributes[itemDependenciesKey]

internal val ApplicationCall.mediaDependencies: MediaRouteDependencies
    get() = application.attributes[mediaDependenciesKey]
