package f95gm.server.state.runtime

import f95gm.server.http.request.MutationService
import f95gm.server.persistence.ServerRepositories
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.state.runtime.session.SessionRegistry
import f95gm.server.state.runtime.session.SessionService

internal class ServerPersistenceServices(
    private val database: DatabaseExecutor,
    sessions: SessionRegistry,
    clock: ServerClock,
    mutationService: MutationService,
    repositories: ServerRepositories? = null
) {
    val sessionService = SessionService(database, sessions, clock)
    val repositories: ServerRepositories = repositories ?: ServerRepositories(database)
    val applicationServices: RouteApplicationServices by lazy {
        DefaultRouteApplicationServices(this.repositories, mutationService)
    }
}
