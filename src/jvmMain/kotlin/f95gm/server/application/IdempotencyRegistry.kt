package f95gm.server.application

import f95gm.server.application.port.IdempotencyStore

import f95gm.server.application.port.IdempotencyKey
import f95gm.server.application.port.IdempotencyFingerprint
import f95gm.server.application.port.CachedResponse
import f95gm.server.application.port.IdempotencyLookup
import f95gm.server.application.port.IdempotencyClaimResult
import f95gm.server.kernel.EntryCapacity
import f95gm.server.kernel.BoundedAccessMap
import f95gm.server.kernel.MonotonicMillis
import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.saturatingAdd
import f95gm.server.platform.SystemServerClock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class IdempotencyRegistry(
    maxEntries: Int = 4_096,
    private val clock: ServerClock = SystemServerClock,
    private val ttl: Duration = 10.minutes,
    private val registrySnapshots: RegistrySnapshotCoordinator = RegistrySnapshotCoordinator()
) : IdempotencyStore {
    private val capacity = EntryCapacity(maxEntries)
    internal sealed interface EntryState {
        val expiresAtMonotonicMillis: MonotonicMillis
        val fingerprint: IdempotencyFingerprint

        data class InProgress(override val expiresAtMonotonicMillis: MonotonicMillis, override val fingerprint: IdempotencyFingerprint) : EntryState
        data class Completed(
            override val expiresAtMonotonicMillis: MonotonicMillis,
            override val fingerprint: IdempotencyFingerprint,
            val response: CachedResponse
        ) : EntryState

        data class Failed(override val expiresAtMonotonicMillis: MonotonicMillis, override val fingerprint: IdempotencyFingerprint) : EntryState
    }

    private val entries = BoundedAccessMap<IdempotencyKey, EntryState>(capacity)
    val size: Int get() = registrySnapshots.withLock { entries.size }

    init {
        require(ttl.isPositive())
    }

    override fun claim(key: IdempotencyKey, fingerprint: IdempotencyFingerprint): Boolean {
        return claimResult(key, fingerprint) is IdempotencyClaimResult.Claimed
    }

    override fun claimResult(key: IdempotencyKey, fingerprint: IdempotencyFingerprint): IdempotencyClaimResult {
        return registrySnapshots.withLock {
            val now = clock.monotonicMillisTimestamp()
            removeExpired(now)
            val existing = entries[key]
            if (existing != null && IdempotencyEntryPolicy.canRetry(existing, fingerprint)) entries.remove(key)
            else if (existing != null) return@withLock if (existing.fingerprint == fingerprint) {
                IdempotencyClaimResult.InProgress
            } else {
                IdempotencyClaimResult.FingerprintMismatch
            }
            if (entries.size >= capacity.value) {
                val terminalEntry = entries.snapshot().firstOrNull { it.value !is EntryState.InProgress }
                if (terminalEntry != null) entries.remove(terminalEntry.key)
                else return@withLock IdempotencyClaimResult.InProgress
            }
            entries.put(key, EntryState.InProgress(MonotonicMillis(saturatingAdd(now.value, ttl.inWholeMilliseconds)), fingerprint))
            IdempotencyClaimResult.Claimed
        }
    }

    override fun lookup(key: IdempotencyKey, fingerprint: IdempotencyFingerprint): CachedResponse? {
        return when (val result = lookupResult(key, fingerprint)) {
            is IdempotencyLookup.Completed -> result.response
            IdempotencyLookup.Missing,
            IdempotencyLookup.Expired,
            IdempotencyLookup.FingerprintMismatch,
            IdempotencyLookup.InProgress,
            IdempotencyLookup.Failed -> null
        }
    }

    fun lookupResult(key: IdempotencyKey, fingerprint: IdempotencyFingerprint): IdempotencyLookup {
        registrySnapshots.withLock {
            val entry = entries[key] ?: return IdempotencyLookup.Missing
            if (entry.expiresAtMonotonicMillis <= clock.monotonicMillisTimestamp()) {
                entries.remove(key)
                return IdempotencyLookup.Expired
            }
            if (entry.fingerprint != fingerprint) return IdempotencyLookup.FingerprintMismatch
            return when (entry) {
                is EntryState.Completed -> IdempotencyLookup.Completed(entry.response)
                is EntryState.InProgress -> IdempotencyLookup.InProgress
                is EntryState.Failed -> IdempotencyLookup.Failed
            }
        }
    }

    override fun hasFingerprintMismatch(key: IdempotencyKey, fingerprint: IdempotencyFingerprint): Boolean = registrySnapshots.withLock {
        entries[key]?.let { it.fingerprint != fingerprint } == true
    }

    override fun complete(key: IdempotencyKey, fingerprint: IdempotencyFingerprint, response: CachedResponse) {
        registrySnapshots.withLock {
            val entry = entries[key] ?: return
            if (entry.expiresAtMonotonicMillis <= clock.monotonicMillisTimestamp()) entries.remove(key)
            else if (entry.fingerprint == fingerprint) entries.put(
                key,
                EntryState.Completed(entry.expiresAtMonotonicMillis, fingerprint, response)
            )
        }
    }

    fun complete(key: IdempotencyKey, response: CachedResponse) = complete(key, IdempotencyFingerprint(""), response)

    override fun release(key: IdempotencyKey) {
        registrySnapshots.withLock {
            val entry = entries[key] ?: return
            if (IdempotencyEntryPolicy.retainAfterRelease(entry.expiresAtMonotonicMillis.value, clock.monotonicMillisTimestamp().value)) {
                entries.put(key, EntryState.Failed(entry.expiresAtMonotonicMillis, entry.fingerprint))
            } else {
                entries.remove(key)
            }
        }
    }

    /** Explicit maintenance hook for low-traffic periods where no claims trigger lazy cleanup. */
    fun sweepExpired(): Int = registrySnapshots.withLock {
        removeExpired(clock.monotonicMillisTimestamp())
    }

    private fun removeExpired(now: MonotonicMillis): Int =
        entries.removeExpired { entry -> entry.expiresAtMonotonicMillis <= now }

}
