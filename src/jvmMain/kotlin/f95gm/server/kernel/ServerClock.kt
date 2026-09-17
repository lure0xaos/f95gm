package f95gm.server.kernel

import f95gm.server.model.EpochMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

@JvmInline
internal value class MonotonicNanos(val value: Long)

internal data class MonotonicMillis(val value: Long) : Comparable<MonotonicMillis> {
    override fun compareTo(other: MonotonicMillis): Int = value.compareTo(other.value)
}

internal data class ClockSnapshot(
    val epochMillis: EpochMillis,
    val monotonicNanos: MonotonicNanos
)

internal interface WallClock {
    fun epochMillis(): EpochMillis
}

internal interface MonotonicClock {
    fun monotonicTimestamp(): MonotonicNanos
    fun monotonicMillisTimestamp(): MonotonicMillis
}

internal interface ServerClock : WallClock, MonotonicClock {
    fun elapsedSince(start: MonotonicNanos): Duration =
        (monotonicTimestamp().value - start.value).coerceAtLeast(0L).nanoseconds
    fun snapshot(): ClockSnapshot = ClockSnapshot(epochMillis(), monotonicTimestamp())
}
