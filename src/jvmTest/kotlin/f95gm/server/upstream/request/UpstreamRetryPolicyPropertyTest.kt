package f95gm.server.upstream.request

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import f95gm.domain.api.RetryJitter
import f95gm.server.model.EpochMillis

class UpstreamRetryPolicyPropertyTest {
    @Test
    fun retrySelectionIsBoundedAcrossMethodsStatusesAndAttempts() {
        val methods = listOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete)
        val statuses = listOf(HttpStatusCode(408, "timeout"), HttpStatusCode(429, "rate"), HttpStatusCode.InternalServerError, HttpStatusCode.OK)
        listOf(1, 2, 3, 5).forEach { maxAttempts ->
            val policy = UpstreamRetryPolicy(
                UpstreamPolicy(maxAttempts = maxAttempts, retryBaseDelay = 10.milliseconds),
                jitter = RetryJitter { _, _ -> 0.milliseconds }
            )
            methods.forEach { method ->
                (0..maxAttempts + 2).forEach { attempt ->
                    assertFalse(policy.shouldRetryTransport(method, attempt) && method != HttpMethod.Get)
                    assertFalse(policy.shouldRetryTransport(method, attempt) && attempt >= maxAttempts - 1)
                    statuses.forEach { status ->
                        assertFalse(policy.shouldRetry(method, status, attempt) && status == HttpStatusCode.OK)
                    }
                }
            }
        }
    }

    @Test
    fun retryDelaysRemainNonNegativeAndRespectRetryAfterBounds() {
        val policy = UpstreamRetryPolicy(
            UpstreamPolicy(retryBaseDelay = 10.milliseconds, retryMaxDelay = 100.milliseconds),
            jitter = RetryJitter { attempt, _ -> attempt.toLong().milliseconds }
        )
        (0..10).forEach { attempt ->
            assertTrue(policy.ioDelay(attempt) >= 0.milliseconds)
            assertTrue(policy.httpDelay(headersOf("Retry-After", "999"), attempt, EpochMillis(0L)) <= 100.milliseconds)
        }
    }

}
