package f95gm.server.routes.catalog

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey

private val catalogDependenciesKey = AttributeKey<CatalogRouteDependencies>("f95gm.catalogDependencies")

internal fun Application.installCatalogDependencies(dependencies: CatalogRouteDependencies) {
    attributes.put(catalogDependenciesKey, dependencies)
}

internal val ApplicationCall.catalogDependencies: CatalogRouteDependencies
    get() = application.attributes[catalogDependenciesKey]
