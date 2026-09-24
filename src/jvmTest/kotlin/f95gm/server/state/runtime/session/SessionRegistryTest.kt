package f95gm.server.state.runtime.session

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SessionRegistryTest {
    @Test
    fun evictsIdleSessionsAndKeepsRecentlyUsedSessions() {
        val registry = SessionRegistry()
        val old = F95SessionId("old")
        val recent = F95SessionId("recent")
        registry.put(old, UpstreamSession(old, F95AccountKey("old")), nowMillis = 0)
        registry.put(recent, UpstreamSession(recent, F95AccountKey("recent")), nowMillis = 9_500)
        registry.get(recent, nowMillis = 10_000)

        assertEquals(1, registry.evictIdle(nowMillis = 10_500, idleMillis = 1_000))
        assertNull(registry.get(old, nowMillis = 10_500))
        assertEquals("recent", registry.get(recent, nowMillis = 10_500)?.accountKey?.value)
    }

    @Test
    fun enforcesMaximumSessionCountByEvictingTheLeastRecentlyUsedEntry() {
        val registry = SessionRegistry(maxEntries = 2)
        val first = F95SessionId("first")
        val second = F95SessionId("second")
        val third = F95SessionId("third")
        registry.put(first, UpstreamSession(first, F95AccountKey("first")), nowMillis = 1)
        registry.put(second, UpstreamSession(second, F95AccountKey("second")), nowMillis = 2)
        registry.get(second, nowMillis = 3)
        registry.put(third, UpstreamSession(third, F95AccountKey("third")), nowMillis = 4)
        assertNull(registry.get(first, nowMillis = 4))
        assertNotNull(registry.get(second, nowMillis = 4))
        assertNotNull(registry.get(third, nowMillis = 4))
    }
}
