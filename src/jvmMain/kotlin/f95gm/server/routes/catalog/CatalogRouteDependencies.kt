package f95gm.server.routes.catalog

import f95gm.server.routes.ports.CatalogRequestPolicy
import f95gm.server.routes.ports.UpstreamRouteDependencies

internal interface CatalogRouteDependencies : UpstreamRouteDependencies {
    val catalogService: CatalogApplicationService
    val catalogOptionsService: CatalogOptionsApplicationService
    val catalogRequestPolicy: CatalogRequestPolicy
}
