package f95gm.server.state.runtime

import f95gm.server.kernel.ServerClock

import f95gm.server.persistence.ServerRepositories
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.persistence.sessions.ExposedSessionRepository
import f95gm.server.application.*
import f95gm.server.application.port.RouteApplicationServices
import f95gm.server.application.port.MutationAuditSink
import f95gm.server.observability.adapter.StructuredMutationAuditSink
import f95gm.server.routes.ports.*

internal class ServerPersistenceServices(
    private val database: TransactionBoundary,
    sessions: SessionRegistry,
    clock: ServerClock,
    mutationService: IdempotencyApplicationService,
    repositories: RepositoryDependency = RepositoryDependency.Default,
    private val auditSink: MutationAuditSink = StructuredMutationAuditSink
) {
    val sessionService = SessionService(ExposedSessionRepository(database, clock), sessions)
    val repositories: ServerRepositories = when (repositories) {
        RepositoryDependency.Default -> ServerRepositories(database)
        is RepositoryDependency.Provided -> repositories.value
    }
    val applicationServices: RouteApplicationServices =
        DefaultRouteApplicationServices(this.repositories, mutationService, auditSink)
}
