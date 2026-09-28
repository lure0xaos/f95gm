package f95gm.server.upstream.request

import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.MonotonicNanos
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/** Captures one monotonic start point and provides a consistent terminal duration. */
internal class UpstreamRequestTiming private constructor(
    private val clock: ServerClock,
    private val startedAt: MonotonicNanos
) {
    fun elapsed(): Duration =
        clock.elapsedSince(startedAt)

    companion object {
        fun start(clock: ServerClock): UpstreamRequestTiming =
            UpstreamRequestTiming(clock, clock.monotonicTimestamp())
    }
}
