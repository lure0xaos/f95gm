package f95gm.server.cache

import f95gm.server.kernel.MonotonicMillis

@JvmInline
internal value class AccessSequence(val value: Long) : Comparable<AccessSequence> {
    override fun compareTo(other: AccessSequence): Int = value.compareTo(other.value)
}

internal data class CacheEntry<V>(val value: V, val expiresAt: MonotonicMillis, val lastAccess: AccessSequence)

internal sealed interface CacheLookup<out V> {
    data class Hit<V>(val value: V) : CacheLookup<V>
    data object Miss : CacheLookup<Nothing>
}
