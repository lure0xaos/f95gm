package f95gm.server.upstream.request

import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import f95gm.server.model.EpochMillis
import f95gm.domain.api.RetryJitter
import kotlin.time.Duration.Companion.seconds

class UpstreamRetryPolicyTest {
    private val policy = UpstreamRetryPolicy(
        UpstreamPolicy(maxAttempts = 3, retryBaseDelay = 100.milliseconds, retryMaxDelay = 500.milliseconds),
        jitter = RetryJitter { _, _ -> 5.milliseconds }
    )

    @Test
    fun retriesOnlyGetRequestsForRetryableStatusesWithinTheAttemptBudget() {
        assertTrue(policy.shouldRetry(HttpMethod.Get, HttpStatusCode.ServiceUnavailable, 0))
        assertFalse(policy.shouldRetry(HttpMethod.Post, HttpStatusCode.ServiceUnavailable, 0))
        assertFalse(policy.shouldRetry(HttpMethod.Get, HttpStatusCode.OK, 0))
        assertFalse(policy.shouldRetry(HttpMethod.Get, HttpStatusCode.ServiceUnavailable, 2))
    }

    @Test
    fun transportRetriesUseTheSameMethodAndAttemptBudgetPolicy() {
        assertTrue(policy.shouldRetryTransport(HttpMethod.Get, 0))
        assertTrue(policy.shouldRetryTransport(HttpMethod.Get, 1))
        assertFalse(policy.shouldRetryTransport(HttpMethod.Get, 2))
        assertFalse(policy.shouldRetryTransport(HttpMethod.Post, 0))
    }

    @Test
    fun honorsRetryAfterSecondsAndCapsTheServerProvidedDelay() {
        assertEquals(500.milliseconds, policy.httpDelay(headersOf(HttpHeaders.RetryAfter, "2"), 0, EpochMillis(0L)))
        assertEquals(500.milliseconds, policy.httpDelay(headersOf(HttpHeaders.RetryAfter, "99"), 0, EpochMillis(0L)))
        assertEquals(105.milliseconds, policy.httpDelay(headersOf(), 0, EpochMillis(0L)))
        assertEquals(105.milliseconds, policy.ioDelay(0))
    }

    @Test
    fun parsesHttpDateRetryAfterAndFallsBackForMalformedValues() {
        val datePolicy = UpstreamRetryPolicy(
            UpstreamPolicy(maxAttempts = 3, retryBaseDelay = 100.milliseconds, retryMaxDelay = 10_000.milliseconds),
            jitter = RetryJitter { _, _ -> 5.milliseconds }
        )

        assertEquals(
            1.seconds,
            datePolicy.httpDelay(headersOf(HttpHeaders.RetryAfter, "Thu, 01 Jan 1970 00:00:06 GMT"), 0, EpochMillis(5_000L))
        )
        assertEquals(
            105.milliseconds,
            datePolicy.httpDelay(headersOf(HttpHeaders.RetryAfter, "not-a-date"), 0, EpochMillis(0L))
        )
    }
}
