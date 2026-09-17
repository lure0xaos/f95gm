package f95gm.server.routes.item

import f95gm.server.routes.ports.UpstreamRouteDependencies

internal interface ItemRouteDependencies : UpstreamRouteDependencies {
    val itemService: ItemApplicationService
}
