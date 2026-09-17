package f95gm.server.application

import f95gm.server.persistence.ServerRepositories
import f95gm.server.application.port.RouteApplicationServices
import f95gm.server.application.port.MutationAuditSink
import f95gm.server.application.port.NoopMutationAuditSink

internal class DefaultRouteApplicationServices(
    repositories: ServerRepositories,
    override val mutations: IdempotencyApplicationService,
    auditSink: MutationAuditSink = NoopMutationAuditSink
) : RouteApplicationServices {
    override val marks = MarkApplicationService(repositories.marks, auditSink)
    override val savedFilters = FilterApplicationService(repositories.savedFilters, auditSink)
}
