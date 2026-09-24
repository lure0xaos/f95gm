package f95gm.server.state.runtime.cache

internal fun interface EvictionPolicy<K, V> {
    fun select(entries: Collection<Map.Entry<K, V>>): K?
}

internal fun <K, V> leastRecentlyUsed(access: (V) -> Long): EvictionPolicy<K, V> =
    EvictionPolicy { entries -> entries.minByOrNull { access(it.value) }?.key }
