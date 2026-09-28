package f95gm.server.state.runtime

import f95gm.server.kernel.ServerClock

import kotlin.time.Duration

/** Deterministic clock for expiry and retry tests; avoids sleeps and wall-clock races. */
internal class MutableServerClock(
    initialMillis: Long = 0L,
    initialMonotonicNanos: Long = 0L
) : ServerClock {
    var millis: Long = initialMillis
    var monotonicNanosValue: Long = initialMonotonicNanos

    override fun epochMillis() = f95gm.server.model.EpochMillis(millis)
    override fun monotonicTimestamp() = f95gm.server.kernel.MonotonicNanos(monotonicNanosValue)
    override fun monotonicMillisTimestamp() = f95gm.server.kernel.MonotonicMillis(monotonicNanosValue / 1_000_000L)

    fun advance(duration: Duration) {
        millis += duration.inWholeMilliseconds
        monotonicNanosValue += duration.inWholeNanoseconds
    }
}
