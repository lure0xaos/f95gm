package f95gm.server.state.runtime

import f95gm.server.http.request.MutationService
import f95gm.server.persistence.ServerRepositories

internal class DefaultRouteApplicationServices(
    repositories: ServerRepositories,
    override val mutations: MutationService
) : RouteApplicationServices {
    override val marks = repositories.marks
    override val savedFilters = repositories.savedFilters
}
