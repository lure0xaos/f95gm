package f95gm.server.state.runtime.session

import f95gm.domain.values.F95SessionId
import f95gm.server.config.logging.logger
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.sessions.ExposedSessionRepository
import f95gm.server.persistence.sessions.SessionRepository
import f95gm.server.state.runtime.ServerClock

internal class SessionService(
    database: DatabaseExecutor,
    val registry: SessionRegistry,
    private val clock: ServerClock
) {
    private val repository: SessionRepository = ExposedSessionRepository(database, clock)
    suspend fun persist(id: F95SessionId, session: UpstreamSession) {
        repository.persist(id, session)
        logger.debug { "event=session_persisted" }
    }

    suspend fun remove(id: F95SessionId) {
        repository.remove(id)
        logger.debug { "event=session_removed" }
    }

    suspend fun restore() {
        repository.restore(registry)
        logger.info { "event=sessions_restored" }
    }

    fun get(id: F95SessionId): UpstreamSession? = registry.get(id)
    fun put(id: F95SessionId, session: UpstreamSession) = registry.put(id, session)
    fun removeInMemory(id: F95SessionId) = registry.remove(id)
    fun evictIdle(nowMillis: Long, idleMillis: Long): Int = registry.evictIdle(nowMillis, idleMillis)
}
