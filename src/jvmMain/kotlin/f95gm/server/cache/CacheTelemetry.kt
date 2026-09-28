package f95gm.server.cache

import f95gm.server.kernel.ConsistentSnapshotSequence
import f95gm.server.kernel.MetricCounter

/** Owns cache-wide counters so storage and loading code do not own metrics state. */
internal class CacheTelemetry(private val updateSequence: ConsistentSnapshotSequence) {
    private val hits = MetricCounter()
    private val misses = MetricCounter()
    private val evictions = MetricCounter()
    private val expiredEvictions = MetricCounter()
    private val shutdownRejections = MetricCounter()
    private val waiterCancellations = MetricCounter()
    private val activeLoadInvalidations = MetricCounter()
    private val staleLoadCompletions = MetricCounter()

    fun recordHit() = update { hits.increment() }
    fun recordMiss() = update { misses.increment() }
    fun recordEviction(expired: Boolean) {
        update { if (expired) expiredEvictions.increment() else evictions.increment() }
    }

    fun recordShutdownRejection() = update { shutdownRejections.increment() }
    fun recordWaiterCancellation(count: Int = 1) {
        update { waiterCancellations.add(count.toLong()) }
    }
    fun recordActiveLoadInvalidation(count: Int = 1) = update { activeLoadInvalidations.add(count.toLong()) }
    fun recordStaleLoadCompletion() = update { staleLoadCompletions.increment() }

    fun snapshot(): CacheStats {
        val result = updateSequence.read(3) {
            snapshotValue()
        }
        return result.value.copy(countersConsistent = result.consistent)
    }

    fun snapshotValue(): CacheStats = CacheStats(
        hits.get(), misses.get(), evictions.get(), expiredEvictions.get(),
        shutdownRejections.get(), waiterCancellations.get(), activeLoadInvalidations.get(),
        staleLoadCompletions.get()
    )

    private inline fun update(block: () -> Unit) = updateSequence.withUpdate(block)
}
