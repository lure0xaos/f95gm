package f95gm.server.cache

import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.MonotonicMillis
import f95gm.server.kernel.EntryCapacity
import f95gm.server.platform.SystemServerClock
import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.AwaitableLifecycleResource
import f95gm.server.kernel.LifecycleGate
import f95gm.server.kernel.ServerInvariantViolation
import f95gm.server.kernel.shutdownTimeoutViolation
import f95gm.server.kernel.remainingShutdownTimeout
import f95gm.server.kernel.ConsistentSnapshotSequence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

internal class BoundedTtlCache<K, V>(
    maxEntries: Int,
    private val ttl: kotlin.time.Duration,
    private val clock: ServerClock = SystemServerClock,
    private val evictionPolicy: EvictionPolicy<K, CacheEntry<V>> = leastRecentlyUsedComparable { it.lastAccess },
    private val onEvent: (CacheEvent) -> Unit = CacheLoggingEventSink::publish,
    private val namespaceOf: (K) -> CacheNamespace = { CacheNamespaces.DEFAULT },
    private val shutdownTimeout: kotlin.time.Duration = 10.seconds
) : ManagedCache<K, V> {
    init { require(shutdownTimeout.isPositive()) }
    private val capacity = EntryCapacity(maxEntries)
    private val expirationPolicy = CacheExpirationPolicy(CacheTtl(ttl))

    companion object {
        private const val MAX_TRACKED_NAMESPACES = 256
        fun <K, V> withPolicy(
            policy: CachePolicy,
            clock: ServerClock = SystemServerClock,
            evictionPolicy: EvictionPolicy<K, CacheEntry<V>> = leastRecentlyUsedComparable { it.lastAccess },
            onEvent: (CacheEvent) -> Unit = {},
            namespaceOf: (K) -> CacheNamespace = { CacheNamespaces.DEFAULT }
        ): BoundedTtlCache<K, V> = BoundedTtlCache(
            policy.maxEntries, policy.ttl, clock, evictionPolicy, onEvent, namespaceOf
        )
    }

    private val store = CacheStore<K, V>()
    private val lifecycle = LifecycleGate()

    override val isClosed: Boolean get() = lifecycle.isClosed
    private val cacheLock = ResourceLock()
    private val metricsSequence = ConsistentSnapshotSequence()
    private val telemetry = CacheTelemetry(metricsSequence)
    private val synchronousLoads = CacheLoadCoordinator<K, V>(
        onWaiterCancellation = telemetry::recordWaiterCancellation,
        onActiveLoadInvalidation = telemetry::recordActiveLoadInvalidation,
        onStaleCompletion = telemetry::recordStaleLoadCompletion
    )
    private val asynchronousLoads = AsyncCacheLoadCoordinator<K>(
        onShutdownRejection = telemetry::recordShutdownRejection,
        onWaiterCancellation = telemetry::recordWaiterCancellation,
        onActiveLoadInvalidation = telemetry::recordActiveLoadInvalidation
    )
    private val namespaceTelemetry = CacheNamespaceTelemetry(MAX_TRACKED_NAMESPACES, metricsSequence)
    private val lookupTelemetry = CacheLookupTelemetry(telemetry, namespaceTelemetry, namespaceOf)
    private val invalidationGeneration = CacheInvalidationGeneration()
    private val eventSink = CacheEventSink(onEvent)
    private val evictionController = CacheEvictionController(
        capacity.value, store, evictionPolicy, namespaceOf, telemetry, namespaceTelemetry, eventSink
    )
    private val entries = CacheEntryCoordinator(
        store, expirationPolicy, cacheLock, evictionController, invalidationGeneration
    )
    private val snapshotCollector = CacheStatisticsSnapshotCollector(
        entries,
        clock::monotonicMillisTimestamp,
        namespaceTelemetry,
        telemetry,
        ::trackedNamespace
    )

    override fun getOrPut(key: K, factory: () -> V): V {
        return getOrPutAtInternal(key, clock.monotonicMillisTimestamp(), factory, clock::monotonicMillisTimestamp)
    }

    fun getOrPutAt(
        key: K,
        now: Long,
        factory: () -> V
    ): V = getOrPutAtInternal(key, MonotonicMillis(now), factory) { MonotonicMillis(now) }

    private fun getOrPutAtInternal(
        key: K,
        now: MonotonicMillis,
        factory: () -> V,
        insertionNow: () -> MonotonicMillis
    ): V {
        val cached = lookup(key, now, recordMiss = false)
        if (cached is CacheLookup.Hit) {
            return cached.value
        }
        val load = try {
            synchronousLoads.claim(key)
        } catch (closed: IllegalStateException) {
            telemetry.recordShutdownRejection()
            throw closed
        }
        if (!load.owner) return synchronousLoads.await(load)
        try {
            // A caller may have missed the first lookup while another owner was
            // completing and removing its load. Recheck before starting a second
            // factory invocation.
            val cachedAfterClaim = lookup(key, now, recordMiss = true)
            if (cachedAfterClaim is CacheLookup.Hit) {
                synchronousLoads.complete(key, load, cachedAfterClaim.value)
                return cachedAfterClaim.value
            }
            val generation = invalidationGeneration.current()
            val result = cacheLoad(factory)
            return result.fold(
                onSuccess = { value ->
                    val stored = storeLoadedValue(key, value, now, insertionNow, generation)
                    synchronousLoads.complete(key, load, value)
                    stored
                },
                onFailure = { error ->
                    CacheFailurePolicy.report(trackedNamespace(key), error)
                    rethrowAfterFailure(error) { synchronousLoads.fail(key, load, it) }
                }
            )
        } finally {
            synchronousLoads.ownerFinished(load)
        }
    }

    override fun invalidate(key: K) {
        invalidationGeneration.advance()
        entries.remove(key)
    }

    override fun invalidateIf(predicate: (K) -> Boolean): Int {
        invalidationGeneration.advance()
        return entries.removeIf(predicate)
    }

    override fun invalidateNamespace(namespace: CacheNamespace): Int = invalidateIf { namespaceOf(it) == namespace }

    override fun invalidateAll() {
        invalidationGeneration.advance()
        entries.clear()
        synchronousLoads.clear()
        asynchronousLoads.clear()
    }

    override fun clear() {
        invalidateAll()
    }

    override fun close() {
        if (!lifecycle.beginClose()) return
        invalidationGeneration.advance()
        entries.clear()
        synchronousLoads.close()
        asynchronousLoads.close()
    }

    override suspend fun closeAndJoin() {
        close()
        try {
            withTimeout(remainingShutdownTimeout(shutdownTimeout)) {
                synchronousLoads.closeAndJoin()
                asynchronousLoads.closeAndJoin()
            }
            if (lifecycle.isClosing) lifecycle.complete()
        } catch (timeout: TimeoutCancellationException) {
            val violation = shutdownTimeoutViolation("cache loads", timeout)
            if (lifecycle.isClosing) lifecycle.fail(violation)
            throw violation
        } catch (cancellation: CancellationException) {
            if (lifecycle.isClosing) lifecycle.fail(cancellation)
            throw cancellation
        }
    }

    fun stats(): CacheStats = telemetry.snapshot()

    override fun snapshot(): CacheStatisticsSnapshot = metricsSequence.read(3) {
        snapshotCollector.snapshot()
    }.let { result ->
        CacheStatisticsSnapshot(
            namespaces = result.value.namespaces.mapValues { (_, stats) ->
                stats.copy(countersConsistent = result.consistent)
            },
            totals = result.value.totals.copy(countersConsistent = result.consistent)
        )
    }

    /** Explicit maintenance hook for low-traffic periods where no lookup triggers lazy cleanup. */
    fun sweepExpired(): Int = entries.sweepExpired(clock.monotonicMillisTimestamp())

    /** Returns a bounded, point-in-time inventory for operational namespace diagnostics. */
    private fun trackedNamespace(key: K): CacheNamespace {
        val namespace = namespaceOf(key)
        return namespaceTelemetry.tracked(namespace)
    }

    override suspend fun getOrPutSuspending(key: K, factory: suspend () -> V): V {
        ensureOpen()
        val loadLock = asynchronousLoads.acquire(key)
        return try {
            asynchronousLoads.withLock(loadLock) load@{
                val now = clock.monotonicMillisTimestamp()
                val cached = lookup(key, now, recordMiss = true)
                if (cached is CacheLookup.Hit) {
                    return@load cached.value
                }
                val generation = invalidationGeneration.current()
                val result = cacheLoadSuspending(factory)
                result.getOrElse { error ->
                    error.rethrowIfCancellation()
                    CacheFailurePolicy.report(trackedNamespace(key), error)
                    throw error
                }.let { value -> storeLoadedValue(key, value, now, clock::monotonicMillisTimestamp, generation) }
            }
        } finally {
            asynchronousLoads.release(loadLock)
        }
    }

    private fun lookup(key: K, now: MonotonicMillis, recordMiss: Boolean): CacheLookup<V> {
        ensureOpen()
        return entries.lookup(key, now).also { result ->
            when (result) {
                is CacheLookup.Hit -> lookupTelemetry.hit(key)
                CacheLookup.Miss -> if (recordMiss) lookupTelemetry.miss(key)
            }
        }
    }

    private fun ensureOpen() {
        if (!lifecycle.isOpen) {
            telemetry.recordShutdownRejection()
            error("Cache is closed")
        }
    }

    private fun storeLoadedValue(
        key: K,
        value: V,
        now: MonotonicMillis,
        insertionNow: () -> MonotonicMillis,
        generation: CacheGeneration
    ): V {
        if (!entries.storeIfCurrent(key, value, now, insertionNow, generation)) {
            telemetry.recordStaleLoadCompletion()
        }
        return value
    }

}
