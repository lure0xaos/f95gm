package f95gm.server.persistence.database

import java.sql.SQLTransientException
import f95gm.domain.api.RetryJitter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class DatabaseRetryTest {
    @Test
    fun retryDelayIsBounded() {
        val policy = DatabaseRetryPolicy(retryDelay = 100.milliseconds, maxRetryDelay = 250.milliseconds)

        assertEquals(100.milliseconds, policy.delayFor(1))
        assertEquals(200.milliseconds, policy.delayFor(2))
        assertEquals(250.milliseconds, policy.delayFor(3))
    }

    @Test
    fun jitteredDelayUsesTheSamePolicyForBothExecutionModes() {
        val policy = DatabaseRetryPolicy(retryDelay = 100.milliseconds, maxRetryDelay = 250.milliseconds)
        val jitter = RetryJitter { _, _ -> 20.milliseconds }

        assertEquals(120.milliseconds, policy.delayWithJitter(1, jitter))
        assertEquals(220.milliseconds, policy.delayWithJitter(2, jitter))
        assertEquals(270.milliseconds, policy.delayWithJitter(3, jitter))
    }

    @Test
    fun transientConnectionFailuresRespectRetryLimit() {
        var calls = 0
        val executor = DatabaseExecutor(
            database = { calls++; throw SQLTransientException("temporary") },
            retryPolicy = DatabaseRetryPolicy(maxRetries = 2, retryDelay = Duration.ZERO),
            executionContext = testDatabaseExecutionContext()
        )
        assertFailsWith<SQLTransientException> { executor.transaction { 1 } }
        assertEquals(3, calls)
    }

    @Test
    fun nonTransientFailuresAreNotRetried() {
        var calls = 0
        val executor = DatabaseExecutor(
            database = { calls++; error("permanent") },
            retryPolicy = DatabaseRetryPolicy(maxRetries = 2, retryDelay = Duration.ZERO),
            executionContext = testDatabaseExecutionContext()
        )
        assertFailsWith<IllegalStateException> { executor.transaction { 1 } }
        assertEquals(1, calls)
    }

    @Test
    fun interruptedRetrySleepRestoresInterruptFlagAndStopsRetrying() {
        var calls = 0
        val executor = DatabaseExecutor(
            database = { calls++; throw SQLTransientException("temporary") },
            retryPolicy = DatabaseRetryPolicy(maxRetries = 2, retryDelay = Duration.ZERO),
            delayScheduler = BlockingDelayScheduler { throw InterruptedException("shutdown") },
            executionContext = testDatabaseExecutionContext()
        )

        val failure = assertFailsWith<InterruptedException> { executor.transaction { 1 } }

        assertEquals("shutdown", failure.message)
        assertEquals(1, calls)
        assertTrue(Thread.currentThread().isInterrupted)
        Thread.interrupted()
    }
}
