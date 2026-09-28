package f95gm.server.kernel

import java.util.concurrent.atomic.AtomicLong

/** Small thread-safe counter primitive shared by observability components. */
internal class MetricCounter {
    private val value = AtomicLong()

    fun increment() = value.incrementAndGet()
    fun incrementAndGet(): Long = value.incrementAndGet()
    fun add(delta: Long) = value.addAndGet(delta)
    fun addAndGet(delta: Long): Long = value.addAndGet(delta)
    fun get(): Long = value.get()
}
