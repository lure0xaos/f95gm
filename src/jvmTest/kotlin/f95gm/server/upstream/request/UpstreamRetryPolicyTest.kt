package f95gm.server.upstream.request

import f95gm.server.state.runtime.ServerClock
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class UpstreamRetryPolicyTest {
    private val policy = UpstreamRetryPolicy(
        UpstreamPolicy(maxAttempts = 3, retryBaseDelay = 100.milliseconds, retryMaxDelay = 500.milliseconds),
        object : ServerClock {
            override fun nowMillis() = 1_000L
        },
        jitterMillis = { 5L }
    )

    @Test
    fun retriesOnlyGetRequestsForRetryableStatusesWithinTheAttemptBudget() {
        assertTrue(policy.shouldRetry(HttpMethod.Get, HttpStatusCode.ServiceUnavailable, 0))
        assertFalse(policy.shouldRetry(HttpMethod.Post, HttpStatusCode.ServiceUnavailable, 0))
        assertFalse(policy.shouldRetry(HttpMethod.Get, HttpStatusCode.OK, 0))
        assertFalse(policy.shouldRetry(HttpMethod.Get, HttpStatusCode.ServiceUnavailable, 2))
    }

    @Test
    fun honorsRetryAfterSecondsAndCapsTheServerProvidedDelay() {
        assertEquals(500.milliseconds, policy.httpDelay(headersOf(HttpHeaders.RetryAfter, "2"), 0))
        assertEquals(500.milliseconds, policy.httpDelay(headersOf(HttpHeaders.RetryAfter, "99"), 0))
        assertEquals(105.milliseconds, policy.httpDelay(headersOf(), 0))
        assertEquals(105.milliseconds, policy.ioDelay(0))
    }

    @Test
    fun parsesHttpDateRetryAfterAndFallsBackForMalformedValues() {
        val datePolicy = UpstreamRetryPolicy(
            UpstreamPolicy(maxAttempts = 3, retryBaseDelay = 100.milliseconds, retryMaxDelay = 10_000.milliseconds),
            object : ServerClock {
                override fun nowMillis() = 1_000L
            },
            jitterMillis = { 5L }
        )

        assertEquals(
            5.seconds,
            datePolicy.httpDelay(headersOf(HttpHeaders.RetryAfter, "Thu, 01 Jan 1970 00:00:06 GMT"), 0)
        )
        assertEquals(
            105.milliseconds,
            datePolicy.httpDelay(headersOf(HttpHeaders.RetryAfter, "not-a-date"), 0)
        )
    }
}
