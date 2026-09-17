package f95gm.server.persistence.sessions

import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.kernel.ServerClock
import f95gm.server.session.SessionRegistryPort

internal interface SessionRepository {
    suspend fun persist(id: F95SessionId, session: UpstreamSession)
    suspend fun remove(id: F95SessionId)
    suspend fun restore(registry: SessionRegistryPort)
}

internal class ExposedSessionRepository(
    private val database: TransactionBoundary,
    private val clock: ServerClock
) : SessionRepository {
    override suspend fun persist(id: F95SessionId, session: UpstreamSession) =
        persistSession(database, clock, id, session)

    override suspend fun remove(id: F95SessionId) = removePersistedSession(database, id)
    override suspend fun restore(registry: SessionRegistryPort) = restoreSessions(database, registry, clock)
}
