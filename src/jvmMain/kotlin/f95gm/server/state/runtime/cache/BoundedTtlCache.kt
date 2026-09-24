package f95gm.server.state.runtime.cache

import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.SystemServerClock
import f95gm.server.state.runtime.saturatingAdd

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class BoundedTtlCache<K, V>(
    private val maxEntries: Int,
    private val ttlMillis: Long,
    private val clock: ServerClock = SystemServerClock,
    private val evictionPolicy: EvictionPolicy<K, Entry<V>> = leastRecentlyUsed { it.lastAccess },
    private val onEviction: () -> Unit = {}
) {
    companion object {
        fun <K, V> withTtl(
            maxEntries: Int,
            ttl: kotlin.time.Duration,
            clock: ServerClock = SystemServerClock,
            evictionPolicy: EvictionPolicy<K, Entry<V>> = leastRecentlyUsed { it.lastAccess },
            onEviction: () -> Unit = {}
        ): BoundedTtlCache<K, V> =
            BoundedTtlCache(maxEntries, ttl.inWholeMilliseconds, clock, evictionPolicy, onEviction)
    }

    internal data class Entry<V>(val value: V, val expiresAt: Long, val lastAccess: Long)

    private val entries = LinkedHashMap<K, Entry<V>>()
    private val synchronousLoads = ConcurrentHashMap<K, CompletableFuture<V>>()

    private data class LoadLock(val mutex: Mutex, val users: AtomicLong = AtomicLong())

    private val loadLocks = mutableMapOf<K, LoadLock>()
    private val cacheLock = Any()
    private val hits = AtomicLong()
    private val misses = AtomicLong()
    private val evictions = AtomicLong()
    private val accessSequence = AtomicLong()

    init {
        require(maxEntries > 0)
        require(ttlMillis > 0)
    }

    fun getOrPut(key: K, factory: () -> V): V {
        return getOrPutAtInternal(key, clock.nowMillis(), factory, clock::nowMillis)
    }

    fun getOrPutAt(
        key: K,
        now: Long,
        factory: () -> V
    ): V = getOrPutAtInternal(key, now, factory) { now }

    private fun getOrPutAtInternal(
        key: K,
        now: Long,
        factory: () -> V,
        insertionNow: () -> Long
    ): V {
        synchronized(cacheLock) {
            entries.entries.removeIf { it.value.expiresAt <= now }
            entries[key]?.takeIf { it.expiresAt > now }?.let {
                hits.incrementAndGet()
                entries[key] = it.copy(lastAccess = accessSequence.incrementAndGet())
                return it.value
            }
        }
        val created = CompletableFuture<V>()
        val load = synchronousLoads.putIfAbsent(key, created) ?: created
        if (load !== created) return await(load)
        misses.incrementAndGet()
        try {
            val value = factory()
            synchronized(cacheLock) {
                evictIfNeeded()
                entries[key] = Entry(
                    value,
                    saturatingAdd(insertionNow().coerceAtLeast(now), ttlMillis),
                    accessSequence.incrementAndGet()
                )
            }
            created.complete(value)
            return value
        } catch (error: Exception) {
            created.completeExceptionally(error)
            throw error
        } catch (error: Error) {
            created.completeExceptionally(error)
            throw error
        } finally {
            synchronousLoads.remove(key, created)
        }
    }

    fun invalidate(key: K) {
        synchronized(cacheLock) { entries.remove(key) }
    }

    fun clear() {
        synchronized(cacheLock) { entries.clear() }
        synchronousLoads.values.forEach { it.cancel(false) }
        synchronousLoads.clear()
        synchronized(loadLocks) { loadLocks.clear() }
    }

    fun stats(): CacheStats = CacheStats(hits.get(), misses.get(), evictions.get())

    suspend fun getOrPutSuspending(key: K, factory: suspend () -> V): V {
        val loadLock = synchronized(loadLocks) {
            (loadLocks[key] ?: LoadLock(Mutex()).also { loadLocks[key] = it }).also { it.users.incrementAndGet() }
        }
        return try {
            loadLock.mutex.withLock load@{
                val now = clock.nowMillis()
                val cachedEntry = synchronized(cacheLock) {
                    entries.entries.removeIf { it.value.expiresAt <= now }
                    entries[key]?.takeIf { it.expiresAt > now }?.let {
                        hits.incrementAndGet()
                        entries[key] = it.copy(lastAccess = accessSequence.incrementAndGet())
                        it
                    }
                }
                if (cachedEntry != null) return@load cachedEntry.value
                misses.incrementAndGet()
                val value = factory()
                synchronized(cacheLock) {
                    evictIfNeeded()
                    entries[key] = Entry(
                        value,
                        saturatingAdd(clock.nowMillis().coerceAtLeast(now), ttlMillis),
                        accessSequence.incrementAndGet()
                    )
                }
                value
            }
        } finally {
            if (loadLock.users.decrementAndGet() == 0L) synchronized(loadLocks) { loadLocks.remove(key, loadLock) }
        }
    }

    private fun evictIfNeeded() {
        while (entries.size >= maxEntries) {
            val keyToRemove = evictionPolicy.select(entries.entries) ?: break
            if (entries.remove(keyToRemove) != null) {
                evictions.incrementAndGet()
                onEviction()
            }
        }
    }

    private fun await(load: CompletableFuture<V>): V = try {
        load.get()
    } catch (error: java.util.concurrent.ExecutionException) {
        throw (error.cause ?: error)
    } catch (error: InterruptedException) {
        Thread.currentThread().interrupt()
        throw CompletionException(error)
    }
}
