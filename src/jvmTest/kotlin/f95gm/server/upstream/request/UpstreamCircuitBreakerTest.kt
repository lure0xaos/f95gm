package f95gm.server.upstream.request

import f95gm.server.state.runtime.ServerClock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class UpstreamCircuitBreakerTest {
    @Test
    fun opensAfterRepeatedFailuresAndAllowsAfterCooldown() {
        var now = 0L
        val clock = object : ServerClock {
            override fun nowMillis() = now
        }
        val breaker = UpstreamCircuitBreaker(clock, failureThreshold = 2, openDuration = 100.milliseconds)
        assertTrue(breaker.allow("upstream"))
        breaker.recordFailure("upstream")
        breaker.recordFailure("upstream")
        assertFalse(breaker.allow("upstream"))
        now = 100
        assertTrue(breaker.allow("upstream"))
    }

    @Test
    fun reportsOnlyTransitionsIntoTheOpenState() {
        var openings = 0
        val breaker = UpstreamCircuitBreaker(
            clock = object : ServerClock {
                override fun nowMillis() = 0L
            },
            failureThreshold = 2,
            openDuration = 100.milliseconds,
            onOpened = { openings++ }
        )
        breaker.recordFailure("upstream")
        breaker.recordFailure("upstream")
        breaker.recordFailure("upstream")
        assertTrue(openings == 1)
    }
}
