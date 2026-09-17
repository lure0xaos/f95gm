package f95gm.server.cache

import f95gm.server.kernel.EntryCapacity
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Named cache policies keep capacity and freshness decisions visible at construction sites. */
internal object CachePolicies {
    val CLASSPATH = CachePolicy(EntryCapacity(512), ttl = 5.minutes)
    val ORIGIN = CachePolicy(EntryCapacity(256), ttl = 5.minutes)
    val PUBLIC_HOST = CachePolicy(EntryCapacity(256), ttl = 5.minutes)
    val LIVE_MARK = CachePolicy(EntryCapacity(1_024), ttl = 30.seconds)
}
