package f95gm.server.application

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.sessions.SessionRepository
import f95gm.server.session.SessionLookup
import f95gm.server.session.SessionRegistryPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest

class SessionServiceTest {
    @Test
    fun failedPersistenceDoesNotPublishSessionInMemory() = runTest {
        val id = F95SessionId("browser")
        val session = UpstreamSession(id, F95AccountKey("account"))
        val registry = SessionRegistry()
        val repository = FakeSessionRepository { _, _ -> throw IllegalStateException("database unavailable") }
        val service = SessionService(repository, registry)

        assertFailsWith<IllegalStateException> { service.persistAndRegister(id, session) }

        assertEquals(SessionLookup.Missing, registry.lookup(id))
    }

    @Test
    fun successfulPersistencePublishesSessionInMemory() = runTest {
        val id = F95SessionId("browser")
        val session = UpstreamSession(id, F95AccountKey("account"))
        val registry = SessionRegistry()
        val repository = FakeSessionRepository { persistedId, persistedSession ->
            assertEquals(id, persistedId)
            assertSame(session, persistedSession)
            assertEquals(SessionLookup.Missing, registry.lookup(id))
        }
        val service = SessionService(repository, registry)

        service.persistAndRegister(id, session)

        assertSame(session, (registry.lookup(id) as SessionLookup.Found).session)
    }

    private class FakeSessionRepository(
        private val persistAction: suspend (F95SessionId, UpstreamSession) -> Unit
    ) : SessionRepository {
        override suspend fun persist(id: F95SessionId, session: UpstreamSession) = persistAction(id, session)
        override suspend fun remove(id: F95SessionId) = Unit
        override suspend fun restore(registry: SessionRegistryPort) = Unit
    }
}
