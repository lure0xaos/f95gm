package f95gm.server.persistence.database

import f95gm.domain.api.RetryJitter
import f95gm.server.kernel.RetryDecision
import f95gm.server.kernel.ServerClock
import f95gm.server.observability.DatabaseOperation
import f95gm.server.observability.DatabaseOperationTelemetry
import f95gm.server.observability.databaseDurationMillis
import f95gm.server.platform.SystemServerClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.SQLException
import kotlin.time.Duration

internal class DatabaseExecutor(
    private val database: () -> Database,
    private val retryPolicy: DatabaseRetryPolicy = DatabaseRetryPolicy(),
    private val delayScheduler: BlockingDelayScheduler = InterruptibleBlockingDelayScheduler(),
    private val executionContext: DatabaseExecutionContext,
    private val retryJitter: RetryJitter = RetryJitter { _, _ -> Duration.ZERO },
    private val clock: ServerClock = SystemServerClock,
    private val telemetry: DatabaseOperationTelemetry = DatabaseOperationTelemetry.noop
) : TransactionBoundary {
    private val retryExecutor = DatabaseRetryExecutor(retryPolicy, retryJitter)
    private val operationRunner = DatabaseOperationRunner(clock, telemetry)

    override fun <T> transaction(block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T): T =
        transaction(DatabaseOperation.TRANSACTION, block)

    override fun <T> transaction(
        operation: DatabaseOperation,
        block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T
    ): T {
        val startedAt = clock.monotonicTimestamp()
        var attempts = 0
        return operationRunner.run(operation, startedAt, { attempts }) {
            retryExecutor.blocking(::retryDelay) {
                attempts++
                transaction(database(), statement = block)
            }
        }
    }

    override suspend fun <T> transactionSuspending(
        operation: DatabaseOperation,
        block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T
    ): T = withContext(executionContext.dispatcher) {
        val startedAt = clock.monotonicTimestamp()
        var attempts = 0
        operationRunner.runSuspending(operation, startedAt, { attempts }) {
            retryExecutor.suspending { attempt ->
                attempts = attempt + 1
                try {
                    RetryDecision.Return(transaction(database(), statement = block))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: SQLException) {
                    when (retryPolicy.failureDecision(error, attempt)) {
                        DatabaseRetryPolicy.FailureDecision.Retry ->
                            RetryDecision.Again(retryPolicy.delayWithJitter(attempt + 1, retryJitter))
                        DatabaseRetryPolicy.FailureDecision.Rethrow -> throw error
                    }
                }
            }
        }
    }

    private fun retryDelay(delay: Duration) {
        try {
            delayScheduler.await(delay)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw interrupted
        }
    }
}
