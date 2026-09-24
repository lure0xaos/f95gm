package f95gm.server.state.runtime

import f95gm.server.config.Millis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryExecutorTest {
    @Test
    fun blockingRetryReturnsAfterTransientFailuresAndRecordsBackoff() {
        var calls = 0
        val delays = mutableListOf<Long>()

        val result = retryBlocking(
            maxAttempts = 3,
            delayMillis = { attempt -> attempt * 10L },
            sleep = delays::add,
            operation = { attempt ->
                calls++
                if (attempt < 2) error("temporary")
                "ok"
            },
            isRetryable = { true }
        )

        assertEquals("ok", result)
        assertEquals(3, calls)
        assertEquals(listOf(10L, 20L), delays)
    }

    @Test
    fun blockingRetryStopsAtTheAttemptLimitAndDoesNotRetryPermanentFailures() {
        var calls = 0
        assertFailsWith<IllegalStateException> {
            retryBlocking(
                maxAttempts = 3,
                delayMillis = { 0L },
                sleep = {},
                operation = { calls++; error("permanent") },
                isRetryable = { false }
            )
        }
        assertEquals(1, calls)
    }

    @Test
    fun suspendingRetryClampsNegativeDelaysAndReturnsTheSuccessfulValue() = runTest {
        val delays = mutableListOf<Long>()
        var calls = 0

        val result = retrySuspending(
            maxAttempts = 3,
            pause = delays::add,
            operation = { attempt ->
                calls++
                if (attempt == 0) RetryDecision.Again(Millis(-50))
                else RetryDecision.Return("ok")
            }
        )

        assertEquals("ok", result)
        assertEquals(2, calls)
        assertEquals(listOf(0L), delays)
    }

    @Test
    fun retryRejectsAnEmptyAttemptBudget() {
        assertFailsWith<IllegalArgumentException> {
            retryBlocking(0, { 0L }, {}, { "unused" }, { true })
        }
        assertFailsWith<IllegalArgumentException> {
            runTest { retrySuspending<String>(0) { RetryDecision.Return("unused") } }
        }
    }

    @Test
    fun suspendingRetryPropagatesCancellationWithoutStartingAnotherAttempt() = runTest {
        var calls = 0
        assertFailsWith<CancellationException> {
            retrySuspending<String>(3, pause = { throw CancellationException("shutdown") }) {
                calls++
                RetryDecision.Again(Millis(1))
            }
        }
        assertEquals(1, calls)
    }
}
