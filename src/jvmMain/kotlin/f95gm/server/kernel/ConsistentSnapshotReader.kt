package f95gm.server.kernel

import java.util.concurrent.atomic.AtomicLong

internal data class ConsistentSnapshot<T>(val value: T, val consistent: Boolean)

internal class ConsistentSnapshotSequence {
    private val sequence = AtomicLong()

    fun begin() { sequence.incrementAndGet() }
    fun end() { sequence.incrementAndGet() }
    inline fun <T> withUpdate(update: () -> T): T {
        begin()
        return try {
            update()
        } finally {
            end()
        }
    }
    fun isUnchangedAndStable(before: Long): Boolean = before == sequence.get() && before % 2L == 0L
    fun current(): Long = sequence.get()

    fun <T> read(attempts: Int, reader: () -> T): ConsistentSnapshot<T> {
        var latest = reader()
        repeat(attempts.coerceAtLeast(1)) {
            val before = sequence.get()
            val candidate = reader()
            val consistent = before == sequence.get() && before % 2L == 0L
            latest = candidate
            if (consistent) return ConsistentSnapshot(candidate, true)
        }
        return ConsistentSnapshot(latest, false)
    }
}
