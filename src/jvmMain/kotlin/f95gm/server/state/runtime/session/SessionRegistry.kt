package f95gm.server.state.runtime.session

import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.SystemServerClock
import f95gm.server.state.runtime.cache.EvictionPolicy
import f95gm.server.state.runtime.cache.leastRecentlyUsed

internal class SessionRegistry(
    private val clock: ServerClock = SystemServerClock,
    private val maxEntries: Int = 2_000,
    private val evictionPolicy: EvictionPolicy<F95SessionId, Entry> = leastRecentlyUsed { it.touchedAt }
) {
    internal data class Entry(val session: UpstreamSession, val touchedAt: Long)

    private val values = LinkedHashMap<F95SessionId, Entry>()
    private val mutationLock = Any()

    init {
        require(maxEntries > 0)
    }

    fun get(id: F95SessionId, nowMillis: Long = clock.nowMillis()): UpstreamSession? = synchronized(mutationLock) {
        values[id]?.also { values[id] = it.copy(touchedAt = nowMillis) }?.session
    }

    fun put(id: F95SessionId, session: UpstreamSession, nowMillis: Long = clock.nowMillis()) {
        synchronized(mutationLock) {
            if (!values.containsKey(id) && values.size >= maxEntries) {
                evictionPolicy.select(values.entries)?.let { values.remove(it) }
            }
            values[id] = Entry(session, nowMillis)
        }
    }

    fun remove(id: F95SessionId): UpstreamSession? = synchronized(mutationLock) { values.remove(id)?.session }

    fun evictIdle(nowMillis: Long, idleMillis: Long): Int {
        synchronized(mutationLock) {
            val before = values.size
            values.entries.removeIf { nowMillis - it.value.touchedAt >= idleMillis }
            return before - values.size
        }
    }

    fun forEach(block: (F95SessionId, UpstreamSession) -> Unit) {
        val snapshot = synchronized(mutationLock) { values.map { (id, entry) -> id to entry.session } }
        snapshot.forEach { (id, session) -> block(id, session) }
    }
}
