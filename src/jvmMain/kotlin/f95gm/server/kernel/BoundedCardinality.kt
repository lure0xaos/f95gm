package f95gm.server.kernel

/** Thread-safe admission tracker for bounded metric dimensions. */
internal class BoundedCardinality<T>(maximum: Int) {
    init { require(maximum >= 0) }

    private val admitted = HashSet<T>()
    private val lock = ResourceLock()
    private val maximum = maximum

    fun admit(value: T): Boolean = lock.withLock {
        if (value in admitted) return true
        if (admitted.size >= maximum) return false
        admitted += value
        true
    }

    val size: Int get() = lock.withLock { admitted.size }
}
