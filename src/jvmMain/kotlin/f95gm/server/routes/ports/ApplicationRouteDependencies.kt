package f95gm.server.routes.ports

import f95gm.server.application.port.RouteApplicationServices

import f95gm.server.config.Bytes

internal interface ApplicationRouteDependencies {
    val maxRequestBodyBytes: Bytes
    val catalogRequestPolicy: CatalogRequestPolicy
    val filterRequestPolicy: FilterRequestPolicy
    val applicationServices: RouteApplicationServices
}
