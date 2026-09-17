package f95gm.server.cache


internal fun interface CacheStatisticsProvider {
    fun snapshot(): CacheStatisticsSnapshot
}
internal data class CacheStatisticsSnapshot(
    val namespaces: Map<CacheNamespace, CacheNamespaceStats>,
    val totals: CacheStats,
    val sourceSnapshots: Map<CacheNamespace, CacheStatisticsSnapshot> = emptyMap()
) {
    val namespaceCountersMatchTotals: Boolean
        get() {
            val namespaceHits = namespaces.values.sumOf { it.hits }
            val namespaceMisses = namespaces.values.sumOf { it.misses }
            val namespaceEvictions = namespaces.values.sumOf { it.evictions }
            val namespaceExpiredEvictions = namespaces.values.sumOf { it.expiredEvictions }
            return totals.hits == namespaceHits && totals.misses == namespaceMisses &&
                totals.evictions == namespaceEvictions && totals.expiredEvictions == namespaceExpiredEvictions
        }

    val countersConsistent: Boolean
        get() = totals.countersConsistent && namespaces.values.all { it.countersConsistent } &&
            namespaceCountersMatchTotals && sourceSnapshots.values.all { it.countersConsistent }
}
internal data class CacheStatisticsSource(
    val prefix: CacheNamespace,
    val provider: CacheStatisticsProvider
)

@JvmInline
internal value class CacheNamespace(val value: String) {
    init { require(value.isNotBlank()) }

    fun qualified(child: CacheNamespace): QualifiedCacheNamespace {
        require(':' !in value) { "Cache namespace prefix must be unqualified" }
        require(':' !in child.value) { "Cache namespace child must be unqualified" }
        return QualifiedCacheNamespace("${value}:${child.value}")
    }
}

/** Namespace produced by composing two independently-owned cache namespaces. */
@JvmInline
internal value class QualifiedCacheNamespace(val value: String) {
    init { require(value.count { it == ':' } == 1) }

    fun asCacheNamespace(): CacheNamespace = CacheNamespace(value)
}

/** Stable namespaces used by server caches and their diagnostics. */
internal object CacheNamespaces {
    val DEFAULT = CacheNamespace("default")
    val CLASSPATH = CacheNamespace("classpath")
    val LIVE_MARK = CacheNamespace("live-mark")
    val STATIC = CacheNamespace("static")
    val PUBLIC_HOST = CacheNamespace("public-host")
    val ORIGIN = CacheNamespace("origin")
}

internal class CompositeCacheStatisticsProvider(
    providers: List<CacheStatisticsSource>,
    private val qualifyNamespaces: Boolean = true
) : CacheStatisticsProvider {
    private val providers = providers.toList().also { values ->
        require(values.map { it.prefix }.distinct().size == values.size) { "Duplicate cache namespace prefixes" }
        require(values.all { it.prefix.value.indexOf(':') < 0 }) { "Cache namespace prefixes must be unqualified" }
    }

    override fun snapshot(): CacheStatisticsSnapshot {
        val snapshots = providers.map { it.prefix to it.provider.snapshot() }
        val namespaces = buildMap {
            snapshots.forEach { (prefix, snapshot) ->
                snapshot.namespaces.forEach { (name, stats) ->
                    val cacheNamespace = if (qualifyNamespaces) prefix.qualified(name).asCacheNamespace() else name
                    require(put(cacheNamespace, stats) == null) { "Duplicate cache namespace: ${cacheNamespace.value}" }
                }
            }
        }
        return CacheStatisticsSnapshot(
            namespaces = namespaces,
            totals = snapshots.map { it.second.totals }.fold(CacheStats()) { total, next -> total + next },
            sourceSnapshots = snapshots.toMap()
        )
    }
}

internal data class CacheStats(
    val hits: Long = 0,
    val misses: Long = 0,
    val evictions: Long = 0,
    val expiredEvictions: Long = 0,
    val shutdownRejections: Long = 0,
    val waiterCancellations: Long = 0,
    val activeLoadInvalidations: Long = 0,
    val staleLoadCompletions: Long = 0,
    val countersConsistent: Boolean = true
) {
    val lookups: Long get() = hits + misses
    val hitRate: Double get() = if (lookups == 0L) 0.0 else hits.toDouble() / lookups
    val missRate: Double get() = if (lookups == 0L) 0.0 else misses.toDouble() / lookups

    operator fun plus(other: CacheStats): CacheStats = CacheStats(
        hits = hits + other.hits,
        misses = misses + other.misses,
        evictions = evictions + other.evictions,
        expiredEvictions = expiredEvictions + other.expiredEvictions,
        shutdownRejections = shutdownRejections + other.shutdownRejections,
        waiterCancellations = waiterCancellations + other.waiterCancellations,
        activeLoadInvalidations = activeLoadInvalidations + other.activeLoadInvalidations,
        staleLoadCompletions = staleLoadCompletions + other.staleLoadCompletions,
        countersConsistent = countersConsistent && other.countersConsistent
    )
}

internal data class CacheNamespaceStats(
    val entries: Long,
    val hits: Long,
    val misses: Long,
    val evictions: Long,
    val expiredEvictions: Long,
    val countersConsistent: Boolean = true
) {
    val lookups: Long get() = hits + misses
    val hitRate: Double get() = if (lookups == 0L) 0.0 else hits.toDouble() / lookups
}
