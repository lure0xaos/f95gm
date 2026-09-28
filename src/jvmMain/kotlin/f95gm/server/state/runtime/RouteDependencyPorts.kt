package f95gm.server.state.runtime

import f95gm.server.routes.catalog.CatalogRouteDependencies
import f95gm.server.routes.catalog.installCatalogDependencies
import f95gm.server.routes.ports.ApplicationRouteDependencies
import f95gm.server.routes.ports.installApplicationDependencies
import f95gm.server.routes.filters.FilterRouteDependencies
import f95gm.server.routes.filters.installFilterDependencies
import f95gm.server.routes.item.ItemRouteDependencies
import f95gm.server.routes.item.MediaRouteDependencies
import f95gm.server.routes.item.installItemDependencies
import f95gm.server.routes.item.installMediaDependencies
import f95gm.server.routes.marks.live.MarkRouteDependencies
import f95gm.server.routes.marks.installMarkDependencies
import f95gm.server.routes.session.LoginRouteDependencies
import f95gm.server.routes.session.SessionRouteDependencies
import f95gm.server.routes.session.installLoginDependencies
import f95gm.server.routes.session.installSessionDependencies
import io.ktor.server.application.Application

internal data class RouteDependencyPorts(
    val application: ApplicationRouteDependencies,
    val filter: FilterRouteDependencies,
    val item: ItemRouteDependencies,
    val login: LoginRouteDependencies,
    val catalog: CatalogRouteDependencies,
    val mark: MarkRouteDependencies,
    val media: MediaRouteDependencies,
    val session: SessionRouteDependencies
)

internal fun Application.installRouteDependencies(ports: RouteDependencyPorts) {
    installApplicationDependencies(ports.application)
    installFilterDependencies(ports.filter)
    installItemDependencies(ports.item)
    installLoginDependencies(ports.login)
    installCatalogDependencies(ports.catalog)
    installMarkDependencies(ports.mark)
    installMediaDependencies(ports.media)
    installSessionDependencies(ports.session)
}
