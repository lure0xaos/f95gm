package f95gm.server.kernel

@JvmInline
internal value class EntryCapacity(val value: Int) {
    init { require(value > 0) { "Entry capacity must be positive" } }
}

internal class BoundedAccessMap<K, V>(private val capacity: EntryCapacity) {
    private val values = AccessOrderedStore<K, V>()
    private val lock = ResourceLock()

    val size: Int get() = lock.withLock { values.size }

    operator fun get(key: K): V? = lock.withLock { values[key] }
    fun put(key: K, value: V) = lock.withLock { values.put(key, value); trimToCapacity() }
    fun remove(key: K): V? = lock.withLock { values.remove(key) }
    fun isNotEmpty(): Boolean = lock.withLock { values.isNotEmpty }
    fun removeIf(predicate: (K, V) -> Boolean): Int = lock.withLock {
        val before = values.size
        values.snapshot().filter { predicate(it.key, it.value) }.forEach { values.remove(it.key) }
        return before - values.size
    }
    fun removeExpired(isExpired: (V) -> Boolean): Int =
        removeIf { _, value -> isExpired(value) }
    fun snapshot(): List<AccessOrderedEntry<K, V>> = lock.withLock { values.snapshot() }
    fun clear() = lock.withLock { values.clear() }

    private fun trimToCapacity() {
        while (values.size > capacity.value) values.snapshot().firstOrNull()?.let { values.remove(it.key) }
    }
}
