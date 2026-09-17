package f95gm.server.application

import f95gm.domain.values.F95SessionId
import f95gm.server.observability.*
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.sessions.SessionRepository
import f95gm.server.kernel.MonotonicMillis
import f95gm.server.session.SessionLookup

internal class SessionService(
    private val repository: SessionRepository,
    val registry: SessionRegistry
) {
    suspend fun persistAndRegister(id: F95SessionId, session: UpstreamSession) {
        repository.persist(id, session)
        registry.put(id, session)
        logger.debug { structuredLog(SessionLifecycleEvent(SessionLifecycleEventName.PERSISTED)) }
    }

    suspend fun remove(id: F95SessionId) {
        repository.remove(id)
        logger.debug { structuredLog(SessionLifecycleEvent(SessionLifecycleEventName.REMOVED)) }
    }

    suspend fun restore() {
        repository.restore(registry)
        logger.info { structuredLog(SessionLifecycleEvent(SessionLifecycleEventName.RESTORED)) }
    }

    fun lookup(id: F95SessionId): SessionLookup = registry.lookup(id)
    fun put(id: F95SessionId, session: UpstreamSession) = registry.put(id, session)
    fun removeInMemory(id: F95SessionId) = registry.remove(id)
    fun evictIdle(now: MonotonicMillis, idle: SessionRegistry.SessionIdleTimeout): Int = registry.evictIdle(now, idle).also { count ->
        if (count > 0) logger.debug { structuredLog(SessionEvictionEvent(count)) }
    }
}
