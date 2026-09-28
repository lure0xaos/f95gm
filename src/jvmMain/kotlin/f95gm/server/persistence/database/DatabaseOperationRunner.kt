package f95gm.server.persistence.database

import f95gm.server.kernel.MonotonicNanos
import f95gm.server.kernel.ServerClock
import f95gm.server.observability.DatabaseOperationEvent
import f95gm.server.observability.DatabaseOperationTelemetry
import f95gm.server.observability.databaseDurationMillis
import f95gm.server.observability.DatabaseOperation
import f95gm.server.observability.DatabaseOperationOutcome
import kotlinx.coroutines.CancellationException

/** Keeps timing, outcome telemetry, and exception handling identical for both execution modes. */
internal class DatabaseOperationRunner(
    private val clock: ServerClock,
    private val telemetry: DatabaseOperationTelemetry
) {
    fun <T> run(
        operation: DatabaseOperation,
        startedAt: MonotonicNanos,
        attempts: () -> Int,
        block: () -> T
    ): T = runWithFailureRecording(
        block = { block().also { success(operation, startedAt, attempts()) } },
        onFailure = { failure -> recordFailure(operation, startedAt, attempts(), failure) }
    )

    suspend fun <T> runSuspending(
        operation: DatabaseOperation,
        startedAt: MonotonicNanos,
        attempts: () -> Int,
        block: suspend () -> T
    ): T = runSuspendingWithFailureRecording(
        block = { block().also { success(operation, startedAt, attempts()) } },
        onFailure = { failure -> recordFailure(operation, startedAt, attempts(), failure) }
    )

    fun success(operation: DatabaseOperation, startedAt: MonotonicNanos, attempts: Int) {
        record(operation, startedAt, attempts, DatabaseOperationOutcome.SUCCESS, null)
    }

    fun failure(operation: DatabaseOperation, startedAt: MonotonicNanos, attempts: Int, error: Throwable) {
        record(operation, startedAt, attempts, DatabaseOperationOutcome.FAILURE, error::class.simpleName)
    }

    private fun recordFailure(operation: DatabaseOperation, startedAt: MonotonicNanos, attempts: Int, error: Exception): Nothing {
        failure(operation, startedAt, attempts, error)
        throw error
    }

    /** The only ordinary-failure catch in this runner; cancellation and fatal errors escape unchanged. */
    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> runWithFailureRecording(
        block: () -> T,
        onFailure: (Exception) -> Nothing
    ): T = try {
        block()
    } catch (failure: Exception) {
        onFailure(failure)
    }

    /** Suspend counterpart keeps coroutine cancellation outside the ordinary-failure boundary. */
    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <T> runSuspendingWithFailureRecording(
        crossinline block: suspend () -> T,
        crossinline onFailure: (Exception) -> Nothing
    ): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onFailure(failure)
    }

    private fun record(
        operation: DatabaseOperation,
        startedAt: MonotonicNanos,
        attempts: Int,
        outcome: DatabaseOperationOutcome,
        errorType: String?
    ) {
        telemetry.record(DatabaseOperationEvent(operation, databaseDurationMillis(clock, startedAt), attempts, outcome, errorType))
    }
}
