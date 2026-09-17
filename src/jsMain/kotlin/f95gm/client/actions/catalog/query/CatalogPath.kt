package f95gm.client.actions.catalog.query

import f95gm.client.model.catalog.CatalogState
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.domain.api.ApiPath

internal suspend fun catalogPath(runtime: ClientCatalogActionsPort, state: CatalogState): ApiPath {
    return catalogQuery(runtime, state).toPath()
}
