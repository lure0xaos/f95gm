package f95gm.server.kernel

/** Non-thread-safe access-ordered storage; callers choose the appropriate synchronization boundary. */
internal class AccessOrderedStore<K, V> {
    private val values = LinkedHashMap<K, V>(16, 0.75f, true)

    val size: Int get() = values.size
    val isNotEmpty: Boolean get() = values.isNotEmpty()
    operator fun get(key: K): V? = values[key]
    fun put(key: K, value: V) { values[key] = value }
    fun remove(key: K): V? = values.remove(key)
    fun snapshot(): List<AccessOrderedEntry<K, V>> = values.map { (key, value) -> AccessOrderedEntry(key, value) }
    fun clear() = values.clear()
}

internal data class AccessOrderedEntry<K, V>(val key: K, val value: V)
