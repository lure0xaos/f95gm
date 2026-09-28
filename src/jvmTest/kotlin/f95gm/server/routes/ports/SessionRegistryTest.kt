package f95gm.server.routes.ports

import f95gm.server.application.SessionRegistry
import f95gm.server.session.SessionLookup

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.kernel.MonotonicMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest

class SessionRegistryTest {
    private fun SessionRegistry.session(id: F95SessionId, now: MonotonicMillis? = null): UpstreamSession? {
        val result = now?.let { lookup(id, it) } ?: lookup(id)
        return (result as? SessionLookup.Found)?.session
    }

    @Test
    fun evictsIdleSessionsAndKeepsRecentlyUsedSessions() {
        val registry = SessionRegistry()
        val old = F95SessionId("old")
        val recent = F95SessionId("recent")
        registry.put(old, UpstreamSession(old, F95AccountKey("old")), now = MonotonicMillis(0))
        registry.put(recent, UpstreamSession(recent, F95AccountKey("recent")), now = MonotonicMillis(9_500))
        registry.session(recent, now = MonotonicMillis(10_000))

        assertEquals(1, registry.evictIdle(now = MonotonicMillis(10_500), idle = SessionRegistry.SessionIdleTimeout(1.seconds)))
        assertNull(registry.session(old, now = MonotonicMillis(10_500)))
        assertEquals("recent", registry.session(recent, now = MonotonicMillis(10_500))?.accountKey?.value)
    }

    @Test
    fun enforcesMaximumSessionCountByEvictingTheLeastRecentlyUsedEntry() {
        val registry = SessionRegistry(maxEntries = 2)
        val first = F95SessionId("first")
        val second = F95SessionId("second")
        val third = F95SessionId("third")
        registry.put(first, UpstreamSession(first, F95AccountKey("first")), now = MonotonicMillis(1))
        registry.put(second, UpstreamSession(second, F95AccountKey("second")), now = MonotonicMillis(2))
        registry.session(second, now = MonotonicMillis(3))
        registry.put(third, UpstreamSession(third, F95AccountKey("third")), now = MonotonicMillis(4))
        assertNull(registry.session(first, now = MonotonicMillis(4)))
        assertNotNull(registry.session(second, now = MonotonicMillis(4)))
        assertNotNull(registry.session(third, now = MonotonicMillis(4)))
    }

    @Test
    fun concurrentWritesAndReadsKeepRegistryConsistent() = runTest {
        val registry = SessionRegistry(maxEntries = 64)
        coroutineScope {
            (1..32).map { index ->
                async(Dispatchers.Default) {
                    val id = F95SessionId("concurrent-$index")
                    registry.put(id, UpstreamSession(id, F95AccountKey("account-$index")))
                    assertNotNull(registry.session(id))
                }
            }.awaitAll()
        }

        assertEquals(32, (1..32).count { registry.session(F95SessionId("concurrent-$it")) != null })
    }
}
