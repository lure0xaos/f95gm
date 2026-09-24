package f95gm.server.persistence.sessions

import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.session.SessionRegistry

internal interface SessionRepository {
    suspend fun persist(id: F95SessionId, session: UpstreamSession)
    suspend fun remove(id: F95SessionId)
    suspend fun restore(registry: SessionRegistry)
}

internal class ExposedSessionRepository(
    private val database: DatabaseExecutor,
    private val clock: ServerClock
) : SessionRepository {
    override suspend fun persist(id: F95SessionId, session: UpstreamSession) =
        persistSession(database, clock, id, session)

    override suspend fun remove(id: F95SessionId) = removePersistedSession(database, id)
    override suspend fun restore(registry: SessionRegistry) = restoreSessions(database, registry, clock)
}
