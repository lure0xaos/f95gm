package f95gm.server.app

import f95gm.server.routes.catalog.catalog
import f95gm.server.routes.catalog.catalogOptions
import f95gm.server.routes.filters.*
import f95gm.server.routes.item.item
import f95gm.server.routes.item.media
import f95gm.server.routes.marks.checkedMarks
import f95gm.server.routes.marks.deleteMark
import f95gm.server.routes.marks.marks
import f95gm.server.routes.marks.saveMark
import f95gm.server.routes.session.login
import f95gm.server.routes.session.logout
import f95gm.server.routes.session.sessionStatus
import f95gm.domain.api.ApiMethod
import f95gm.domain.api.ApiOperationKey
import f95gm.domain.api.ApiPath
import f95gm.domain.api.F95ApiPaths
import io.ktor.server.routing.*

/** Handwritten handlers keyed by the typed shared endpoint key. */
private val apiHandlerRegistrations: Map<ApiOperationKey, Route.(ApiPath) -> Unit> = mapOf(
    ApiOperationKey(ApiPath(F95ApiPaths.LOGIN), ApiMethod.POST) to { path -> post(path.value) { login(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.LOGOUT), ApiMethod.POST) to { path -> post(path.value) { logout(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.SESSION), ApiMethod.GET) to { path -> get(path.value) { sessionStatus(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.CATALOG_OPTIONS), ApiMethod.GET) to { path -> get(path.value) { catalogOptions(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.CATALOG), ApiMethod.GET) to { path -> get(path.value) { catalog(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.MARKS), ApiMethod.GET) to { path -> get(path.value) { marks(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.MARKS_CHECK), ApiMethod.GET) to { path -> get(path.value) { checkedMarks(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.MARK_BY_THREAD), ApiMethod.PUT) to { path -> put(path.value) { saveMark(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.MARK_BY_THREAD), ApiMethod.DELETE) to { path -> delete(path.value) { deleteMark(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.FILTERS), ApiMethod.GET) to { path -> get(path.value) { filters(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.FILTERS_EXPORT), ApiMethod.GET) to { path -> get(path.value) { exportFilters(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.FILTERS_IMPORT), ApiMethod.POST) to { path -> post(path.value) { importFilters(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.FILTERS), ApiMethod.POST) to { path -> post(path.value) { saveFilter(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.FILTER_BY_ID), ApiMethod.DELETE) to { path -> delete(path.value) { deleteFilter(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.ITEM_BY_THREAD), ApiMethod.GET) to { path -> get(path.value) { item(call) } },
    ApiOperationKey(ApiPath(F95ApiPaths.MEDIA), ApiMethod.GET) to { path -> get(path.value) { media(call) } }
)

internal val apiRouteHandlers: List<ConfiguredApiEndpoint> = apiHandlerRegistrations.map { (operation, register) ->
    configuredEndpoint(operation, register)
}
