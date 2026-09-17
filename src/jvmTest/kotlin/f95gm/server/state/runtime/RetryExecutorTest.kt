package f95gm.server.state.runtime

import f95gm.server.kernel.retryBlocking
import f95gm.server.kernel.retrySuspending
import f95gm.server.kernel.RetryAttemptLimit
import f95gm.server.kernel.RetryFailureReason
import f95gm.server.kernel.RetryExecutor

import f95gm.server.kernel.RetryDecision
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RetryExecutorTest {
    @Test
    fun blockingRetryReturnsAfterTransientFailuresAndRecordsBackoff() {
        var calls = 0
        val delays = mutableListOf<Long>()

        val result = retryBlocking(
            maxAttempts = RetryAttemptLimit(3),
            delay = { attempt -> (attempt * 10L).milliseconds },
            sleep = { delays += it.inWholeMilliseconds },
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
                maxAttempts = RetryAttemptLimit(3),
                delay = { 0.milliseconds },
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
            maxAttempts = RetryAttemptLimit(3),
            pause = { delays += it.inWholeMilliseconds },
            operation = { attempt ->
                calls++
                if (attempt == 0) RetryDecision.Again((-50).milliseconds)
                else RetryDecision.Return("ok")
            }
        )

        assertEquals("ok", result)
        assertEquals(2, calls)
        assertEquals(listOf(0L), delays)
    }

    @Test
    fun suspendingRetryDoesNotPauseAfterTheFinalAttempt() = runTest {
        var calls = 0
        val delays = mutableListOf<Long>()
        val failure = assertFailsWith<RetryExecutor> {
            retrySuspending<String>(
                maxAttempts = RetryAttemptLimit(1),
                pause = { delays += it.inWholeMilliseconds },
                maxTotalDelay = 0.milliseconds
            ) {
                calls++
                RetryDecision.Again(5.seconds)
            }
        }

        assertEquals(RetryFailureReason.NO_RESULT, failure.reason)
        assertEquals(1, calls)
        assertEquals(emptyList(), delays)
    }

    @Test
    fun retryRejectsAnEmptyAttemptBudget() {
        assertFailsWith<IllegalArgumentException> {
            retryBlocking(RetryAttemptLimit(0), { 0.milliseconds }, {}, { "unused" }, { true })
        }
        assertFailsWith<IllegalArgumentException> {
            runTest { retrySuspending<String>(RetryAttemptLimit(0)) { RetryDecision.Return("unused") } }
        }
    }

    @Test
    fun suspendingRetryPropagatesCancellationWithoutStartingAnotherAttempt() = runTest {
        var calls = 0
        assertFailsWith<CancellationException> {
            retrySuspending<String>(RetryAttemptLimit(3), pause = { throw CancellationException("shutdown") }) {
                calls++
                RetryDecision.Again(1.milliseconds)
            }
        }
        assertEquals(1, calls)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun suspendingRetryUsesVirtualTimeForDefaultBackoff() = runTest {
        var calls = 0
        val result = retrySuspending<String>(RetryAttemptLimit(2)) {
            calls++
            if (calls == 1) RetryDecision.Again(5.seconds) else RetryDecision.Return("ok")
        }

        advanceUntilIdle()
        assertEquals("ok", result)
        assertEquals(2, calls)
        assertEquals(5_000, testScheduler.currentTime)
    }
}
