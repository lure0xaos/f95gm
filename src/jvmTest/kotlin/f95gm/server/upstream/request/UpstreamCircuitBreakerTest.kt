package f95gm.server.upstream.request

import f95gm.server.kernel.ServerClock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class UpstreamCircuitBreakerTest {
    @Test
    fun opensAfterRepeatedFailuresAndAllowsAfterCooldown() {
        var now = 0L
        val clock = object : ServerClock {
            override fun epochMillis() = f95gm.server.model.EpochMillis(now)
            override fun monotonicTimestamp() = f95gm.server.kernel.MonotonicNanos(now * 1_000_000L)
            override fun monotonicMillisTimestamp() = f95gm.server.kernel.MonotonicMillis(now)
        }
        val breaker = UpstreamCircuitBreaker(clock, failureThreshold = 2, openDuration = 100.milliseconds)
        val key = UpstreamCircuitKey("upstream")
        assertTrue(breaker.allow(key))
        breaker.recordFailure(key)
        breaker.recordFailure(key)
        assertFalse(breaker.allow(key))
        now = 100
        assertTrue(breaker.allow(key))
    }

    @Test
    fun reportsOnlyTransitionsIntoTheOpenState() {
        var openings = 0
        val breaker = UpstreamCircuitBreaker(
            clock = object : ServerClock {
                override fun epochMillis() = f95gm.server.model.EpochMillis(0L)
                override fun monotonicTimestamp() = f95gm.server.kernel.MonotonicNanos(0L)
                override fun monotonicMillisTimestamp() = f95gm.server.kernel.MonotonicMillis(0L)
            },
            failureThreshold = 2,
            openDuration = 100.milliseconds,
            onOpened = { openings++ }
        )
        val key = UpstreamCircuitKey("upstream")
        breaker.recordFailure(key)
        breaker.recordFailure(key)
        breaker.recordFailure(key)
        assertTrue(openings == 1)
    }
}
