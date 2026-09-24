package f95gm.server.app

import f95gm.domain.api.F95Api
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
import io.ktor.http.*
import io.ktor.server.routing.*

internal data class ConfiguredApiEndpoint(
    val path: String,
    val method: HttpMethod,
    val register: Route.() -> Unit
) {
    init {
        require(F95Api.endpointDescriptors.any { descriptor ->
            descriptor.path == path && descriptor.methods.any { it.wireValue == method.value }
        }) {
            "Route $method $path is not declared in the shared API contract"
        }
    }
}

internal val configuredApiEndpoints = listOf(
    ConfiguredApiEndpoint(F95Api.LOGIN, HttpMethod.Post) { post(F95Api.LOGIN) { login(call) } },
    ConfiguredApiEndpoint(F95Api.LOGOUT, HttpMethod.Post) { post(F95Api.LOGOUT) { logout(call) } },
    ConfiguredApiEndpoint(F95Api.SESSION, HttpMethod.Get) { get(F95Api.SESSION) { sessionStatus(call) } },
    ConfiguredApiEndpoint(
        F95Api.CATALOG_OPTIONS,
        HttpMethod.Get
    ) { get(F95Api.CATALOG_OPTIONS) { catalogOptions(call) } },
    ConfiguredApiEndpoint(F95Api.CATALOG, HttpMethod.Get) { get(F95Api.CATALOG) { catalog(call) } },
    ConfiguredApiEndpoint(F95Api.MARKS, HttpMethod.Get) { get(F95Api.MARKS) { marks(call) } },
    ConfiguredApiEndpoint(F95Api.MARKS_CHECK, HttpMethod.Get) { get(F95Api.MARKS_CHECK) { checkedMarks(call) } },
    ConfiguredApiEndpoint(F95Api.MARK_BY_THREAD, HttpMethod.Put) { put(F95Api.MARK_BY_THREAD) { saveMark(call) } },
    ConfiguredApiEndpoint(
        F95Api.MARK_BY_THREAD,
        HttpMethod.Delete
    ) { delete(F95Api.MARK_BY_THREAD) { deleteMark(call) } },
    ConfiguredApiEndpoint(F95Api.FILTERS, HttpMethod.Get) { get(F95Api.FILTERS) { filters(call) } },
    ConfiguredApiEndpoint(F95Api.FILTERS_EXPORT, HttpMethod.Get) { get(F95Api.FILTERS_EXPORT) { exportFilters(call) } },
    ConfiguredApiEndpoint(
        F95Api.FILTERS_IMPORT,
        HttpMethod.Post
    ) { post(F95Api.FILTERS_IMPORT) { importFilters(call) } },
    ConfiguredApiEndpoint(F95Api.FILTERS, HttpMethod.Post) { post(F95Api.FILTERS) { saveFilter(call) } },
    ConfiguredApiEndpoint(
        F95Api.FILTER_BY_ID,
        HttpMethod.Delete
    ) { delete(F95Api.FILTER_BY_ID) { deleteFilter(call) } },
    ConfiguredApiEndpoint(F95Api.ITEM_BY_THREAD, HttpMethod.Get) { get(F95Api.ITEM_BY_THREAD) { item(call) } },
    ConfiguredApiEndpoint(F95Api.MEDIA, HttpMethod.Get) { get(F95Api.MEDIA) { media(call) } }
)

internal fun Route.configureApiRoutes() {
    configuredApiEndpoints.forEach { it.register(this) }
}
