package f95gm.server.state.runtime

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class IdempotencyRegistryTest {
    @Test
    fun claimsAKeyOnlyOnceDuringItsRetentionWindow() {
        val registry = IdempotencyRegistry()
        assertTrue(registry.claim("account:key"))
        assertFalse(registry.claim("account:key"))
        assertTrue(registry.claim("account:other"))
    }

    @Test
    fun completedResponseCanBeReplayed() {
        val registry = IdempotencyRegistry()
        val response = IdempotencyRegistry.CachedResponse(200, "{}", "application/json")
        assertTrue(registry.claim("account:key"))
        registry.complete("account:key", response)
        assertEquals(response, registry.lookup("account:key"))
    }

    @Test
    fun expiredClaimsCanBeReusedAndDoNotReplay() {
        var now = 0L
        val clock = object : ServerClock {
            override fun nowMillis() = now
        }
        val registry = IdempotencyRegistry(clock = clock, ttl = 100.milliseconds)
        val response = IdempotencyRegistry.CachedResponse(200, "{}", null)
        assertTrue(registry.claim("account:key"))
        registry.complete("account:key", response)
        now = 101L
        assertEquals(null, registry.lookup("account:key"))
        assertTrue(registry.claim("account:key"))
    }

    @Test
    fun releasedClaimCanBeRetriedAfterAFailedMutation() {
        val registry = IdempotencyRegistry()
        assertTrue(registry.claim("account:key"))
        registry.release("account:key")
        assertTrue(registry.claim("account:key"))
    }

    @Test
    fun fingerprintMismatchCannotReplayOrReclaimACompletedMutation() {
        val registry = IdempotencyRegistry()
        val response = IdempotencyRegistry.CachedResponse(200, "{}", null)
        assertTrue(registry.claim("account:key", "fingerprint-a"))
        registry.complete("account:key", "fingerprint-a", response)

        assertEquals(null, registry.lookup("account:key", "fingerprint-b"))
        assertTrue(registry.hasFingerprintMismatch("account:key", "fingerprint-b"))
        assertFalse(registry.claim("account:key", "fingerprint-b"))
    }

    @Test
    fun inProgressMutationCannotBeReplayedUntilItCompletes() {
        val registry = IdempotencyRegistry()
        assertTrue(registry.claim("account:key", "fingerprint"))

        assertEquals(null, registry.lookup("account:key", "fingerprint"))
        assertFalse(registry.claim("account:key", "fingerprint"))
    }

    @Test
    fun oldestEntryIsEvictedWhenCapacityIsReached() {
        val registry = IdempotencyRegistry(maxEntries = 1)
        assertTrue(registry.claim("account:first"))
        assertTrue(registry.claim("account:second"))
        assertTrue(registry.claim("account:first"))
    }

    @Test
    fun concurrentClaimsAllowOnlyOneMutationOwner() {
        val registry = IdempotencyRegistry()
        val executor = Executors.newFixedThreadPool(8)
        try {
            val results = (1..100).map { executor.submit<Boolean> { registry.claim("account:shared") } }
            assertEquals(1, results.count { it.get(5, TimeUnit.SECONDS) })
        } finally {
            executor.shutdownNow()
        }
    }
}
