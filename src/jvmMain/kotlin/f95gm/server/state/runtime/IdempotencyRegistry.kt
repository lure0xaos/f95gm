package f95gm.server.state.runtime

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class IdempotencyRegistry(
    private val maxEntries: Int = 4_096,
    private val clock: ServerClock = SystemServerClock,
    private val ttl: Duration = 10.minutes
) {
    data class CachedResponse(val status: Int, val body: String, val contentType: String?)
    private sealed interface Entry {
        val expiresAt: Long
        val fingerprint: String

        data class InProgress(override val expiresAt: Long, override val fingerprint: String) : Entry
        data class Completed(
            override val expiresAt: Long,
            override val fingerprint: String,
            val response: CachedResponse
        ) : Entry

        data class Failed(override val expiresAt: Long, override val fingerprint: String) : Entry
    }

    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private val lock = Any()

    init {
        require(maxEntries > 0)
        require(ttl.isPositive())
    }

    fun claim(key: String, fingerprint: String = ""): Boolean {
        synchronized(lock) {
            val now = clock.nowMillis()
            entries.entries.removeIf { it.value.expiresAt <= now }
            val existing = entries[key]
            if (existing is Entry.Failed && existing.fingerprint == fingerprint) entries.remove(key)
            else if (existing != null) return false
            if (entries.size >= maxEntries) entries.entries.firstOrNull()?.let { entries.remove(it.key) }
            entries[key] = Entry.InProgress(saturatingAdd(now, ttl.inWholeMilliseconds), fingerprint)
            return true
        }
    }

    fun lookup(key: String, fingerprint: String = ""): CachedResponse? {
        synchronized(lock) {
            val entry = entries[key] ?: return null
            if (entry.expiresAt <= clock.nowMillis()) {
                entries.remove(key)
                return null
            }
            return (entry as? Entry.Completed)?.takeIf { it.fingerprint == fingerprint }?.response
        }
    }

    fun hasFingerprintMismatch(key: String, fingerprint: String): Boolean = synchronized(lock) {
        entries[key]?.let { it.fingerprint != fingerprint } == true
    }

    fun complete(key: String, fingerprint: String = "", response: CachedResponse) {
        synchronized(lock) {
            val entry = entries[key] ?: return
            if (entry.expiresAt <= clock.nowMillis()) entries.remove(key)
            else if (entry.fingerprint == fingerprint) entries[key] =
                Entry.Completed(entry.expiresAt, fingerprint, response)
        }
    }

    fun complete(key: String, response: CachedResponse) = complete(key, "", response)

    fun release(key: String) {
        synchronized(lock) {
            val entry = entries[key] ?: return
            if (entry.expiresAt > clock.nowMillis()) {
                entries[key] = Entry.Failed(entry.expiresAt, entry.fingerprint)
            } else {
                entries.remove(key)
            }
        }
    }
}
