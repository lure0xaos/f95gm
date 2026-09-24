package f95gm.server.persistence.database

import f95gm.server.state.runtime.retryBlocking
import f95gm.server.state.runtime.saturatingMultiply
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.SQLException
import java.sql.SQLRecoverableException
import java.sql.SQLTransientException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal data class DatabaseRetryPolicy(
    val maxRetries: Int = 2,
    val retryDelay: Duration = 25.milliseconds
) {
    init {
        require(maxRetries >= 0)
        require(!retryDelay.isNegative())
    }
}

internal class DatabaseExecutor(
    private val database: () -> Database,
    private val retryPolicy: DatabaseRetryPolicy = DatabaseRetryPolicy(),
    private val sleep: (Long) -> Unit = Thread::sleep,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Executes one repository operation inside a retryable database transaction. */
    fun <T> transaction(block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T): T {
        return retryBlocking(
            maxAttempts = retryPolicy.maxRetries + 1,
            delayMillis = { attempt ->
                saturatingMultiply(
                    retryPolicy.retryDelay.inWholeMilliseconds,
                    attempt.toLong()
                )
            },
            sleep = ::retrySleep,
            operation = { transaction(database(), statement = block) },
            isRetryable = { it is SQLException && it.isRetryable() }
        )
    }

    suspend fun <T> transactionSuspending(
        block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T
    ): T = withContext(dispatcher) { transaction(block) }

    @Deprecated("Use transaction to make the database boundary explicit", ReplaceWith("transaction(block)"))
    fun <T> query(block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T): T = transaction(block)

    private fun SQLException.isRetryable(): Boolean = this is SQLTransientException || this is SQLRecoverableException

    private fun retrySleep(delayMillis: Long) {
        try {
            sleep(delayMillis)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw interrupted
        }
    }
}
