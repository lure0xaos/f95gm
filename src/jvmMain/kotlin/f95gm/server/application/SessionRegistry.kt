package f95gm.server.application

import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.MonotonicMillis
import f95gm.server.platform.SystemServerClock
import f95gm.server.kernel.EntryCapacity
import f95gm.server.kernel.BoundedAccessMap
import f95gm.server.kernel.elapsedAtLeast
import f95gm.server.session.SessionRegistryPort
import f95gm.server.session.SessionLookup
import kotlin.time.Duration

internal class SessionRegistry(
    private val clock: ServerClock = SystemServerClock,
    maxEntries: Int = 2_000,
    private val registrySnapshots: RegistrySnapshotCoordinator = RegistrySnapshotCoordinator()
) : SessionRegistryPort {
    private val capacity = EntryCapacity(maxEntries)
    @JvmInline
    internal value class SessionIdleTimeout(val duration: Duration) {
        init { require(!duration.isNegative()) }
    }
    internal data class Entry(val session: UpstreamSession, val touchedAt: MonotonicMillis)

    private val values = BoundedAccessMap<F95SessionId, Entry>(capacity)

    val size: Int get() = registrySnapshots.withLock { values.size }

    override fun lookup(id: F95SessionId, now: MonotonicMillis): SessionLookup {
        registrySnapshots.withLock {
            val entry = values[id] ?: return SessionLookup.Missing
            values.put(id, entry.copy(touchedAt = now))
            return SessionLookup.Found(entry.session)
        }
    }

    fun lookup(id: F95SessionId): SessionLookup = lookup(id, clock.monotonicMillisTimestamp())

    override fun put(id: F95SessionId, session: UpstreamSession, now: MonotonicMillis) {
        registrySnapshots.withLock { values.put(id, Entry(session, now)) }
    }

    fun put(id: F95SessionId, session: UpstreamSession) = put(id, session, clock.monotonicMillisTimestamp())

    fun remove(id: F95SessionId): UpstreamSession? = registrySnapshots.withLock { values.remove(id)?.session }

    fun evictIdle(now: MonotonicMillis, idle: SessionIdleTimeout): Int {
        return registrySnapshots.withLock {
            values.removeIf { _, entry -> elapsedAtLeast(now.value, entry.touchedAt.value, idle.duration.inWholeMilliseconds) }
        }
    }

    fun forEach(block: (F95SessionId, UpstreamSession) -> Unit) {
        val snapshot = registrySnapshots.withLock {
            values.snapshot().map { entry -> entry.key to entry.value.session }
        }
        snapshot.forEach { (id, session) -> block(id, session) }
    }
}
