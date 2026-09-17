package f95gm.server.routes.ports

import f95gm.server.application.IdempotencyRegistry
import f95gm.server.application.port.CachedResponse
import f95gm.server.application.port.IdempotencyKey
import f95gm.server.application.port.IdempotencyFingerprint

import f95gm.domain.api.ApiStatusCode
import f95gm.server.kernel.ServerClock

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class IdempotencyRegistryTest {
    private fun key(value: String) = IdempotencyKey(value)
    private fun fingerprint(value: String) = IdempotencyFingerprint(value)

    @Test
    fun claimsAKeyOnlyOnceDuringItsRetentionWindow() {
        val registry = IdempotencyRegistry()
        assertTrue(registry.claim(key("account:key")))
        assertFalse(registry.claim(key("account:key")))
        assertTrue(registry.claim(key("account:other")))
    }

    @Test
    fun completedResponseCanBeReplayed() {
        val registry = IdempotencyRegistry()
        val response = CachedResponse(ApiStatusCode(200), "{}", "application/json")
        assertTrue(registry.claim(key("account:key")))
        registry.complete(key("account:key"), response)
        assertEquals(response, registry.lookup(key("account:key")))
    }

    @Test
    fun expiredClaimsCanBeReusedAndDoNotReplay() {
        var now = 0L
        val clock = object : ServerClock {
            override fun epochMillis() = f95gm.server.model.EpochMillis(now)
            override fun monotonicTimestamp() = f95gm.server.kernel.MonotonicNanos(now * 1_000_000L)
            override fun monotonicMillisTimestamp() = f95gm.server.kernel.MonotonicMillis(now)
        }
        val registry = IdempotencyRegistry(clock = clock, ttl = 100.milliseconds)
        val response = CachedResponse(ApiStatusCode(200), "{}")
        assertTrue(registry.claim(key("account:key")))
        registry.complete(key("account:key"), response)
        now = 101L
        assertEquals(null, registry.lookup(key("account:key")))
        assertTrue(registry.claim(key("account:key")))
    }

    @Test
    fun releasedClaimCanBeRetriedAfterAFailedMutation() {
        val registry = IdempotencyRegistry()
        assertTrue(registry.claim(key("account:key")))
        registry.release(key("account:key"))
        assertTrue(registry.claim(key("account:key")))
    }

    @Test
    fun fingerprintMismatchCannotReplayOrReclaimACompletedMutation() {
        val registry = IdempotencyRegistry()
        val response = CachedResponse(ApiStatusCode(200), "{}")
        assertTrue(registry.claim(key("account:key"), fingerprint("fingerprint-a")))
        registry.complete(key("account:key"), fingerprint("fingerprint-a"), response)

        assertEquals(null, registry.lookup(key("account:key"), fingerprint("fingerprint-b")))
        assertTrue(registry.hasFingerprintMismatch(key("account:key"), fingerprint("fingerprint-b")))
        assertFalse(registry.claim(key("account:key"), fingerprint("fingerprint-b")))
    }

    @Test
    fun inProgressMutationCannotBeReplayedUntilItCompletes() {
        val registry = IdempotencyRegistry()
        assertTrue(registry.claim(key("account:key"), fingerprint("fingerprint")))

        assertEquals(null, registry.lookup(key("account:key"), fingerprint("fingerprint")))
        assertFalse(registry.claim(key("account:key"), fingerprint("fingerprint")))
    }

    @Test
    fun oldestTerminalEntryIsEvictedWhenCapacityIsReached() {
        val registry = IdempotencyRegistry(maxEntries = 1)
        assertTrue(registry.claim(key("account:first")))
        registry.complete(key("account:first"), CachedResponse(ApiStatusCode(200), "{}"))
        assertTrue(registry.claim(key("account:second")))
        assertFalse(registry.claim(key("account:first")))
        assertEquals(
            f95gm.server.application.port.IdempotencyLookup.InProgress,
            registry.lookupResult(key("account:second"), fingerprint(""))
        )
        assertEquals(
            f95gm.server.application.port.IdempotencyLookup.Missing,
            registry.lookupResult(key("account:first"), fingerprint(""))
        )
    }

    @Test
    fun activeClaimsAreNeverEvictedWhenCapacityIsReached() {
        val registry = IdempotencyRegistry(maxEntries = 1)
        assertTrue(registry.claim(key("account:first"), fingerprint("first")))

        assertFalse(registry.claim(key("account:second"), fingerprint("second")))
        assertEquals(
            f95gm.server.application.port.IdempotencyLookup.InProgress,
            registry.lookupResult(key("account:first"), fingerprint("first"))
        )
        assertFalse(registry.claim(key("account:first"), fingerprint("first")))
    }

    @Test
    fun concurrentClaimsAllowOnlyOneMutationOwner() {
        val registry = IdempotencyRegistry()
        val executor = Executors.newFixedThreadPool(8)
        try {
            val results = (1..100).map { executor.submit<Boolean> { registry.claim(key("account:shared")) } }
            assertEquals(1, results.count { it.get(5, TimeUnit.SECONDS) })
        } finally {
            executor.shutdownNow()
        }
    }
}
