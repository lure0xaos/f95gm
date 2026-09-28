package f95gm.server.persistence.database

import f95gm.domain.api.RetryJitter
import java.sql.SQLException
import java.sql.SQLRecoverableException
import java.sql.SQLTransientException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal data class DatabaseRetryPolicy(
    val maxRetries: Int = 2,
    val retryDelay: Duration = 25.milliseconds,
    val maxRetryDelay: Duration = 500.milliseconds,
    val maxTotalRetryDelay: Duration = 2.seconds
) {
    internal sealed interface FailureDecision {
        data object Retry : FailureDecision
        data object Rethrow : FailureDecision
    }

    init {
        require(maxRetries >= 0)
        require(!retryDelay.isNegative())
        require(!maxRetryDelay.isNegative())
        require(maxRetryDelay >= retryDelay)
        require(!maxTotalRetryDelay.isNegative())
    }

    fun delayFor(retryNumber: Int): Duration {
        require(retryNumber > 0)
        return (retryDelay * retryNumber).coerceAtMost(maxRetryDelay)
    }

    fun delayWithJitter(retryNumber: Int, jitter: RetryJitter): Duration =
        (delayFor(retryNumber) + jitter.delay(retryNumber, retryDelay)).coerceAtLeast(Duration.ZERO)

    fun failureDecision(error: Throwable, attempt: Int): FailureDecision =
        if (attempt < maxRetries && error is SQLException && error.isTransientDatabaseFailure()) {
            FailureDecision.Retry
        } else FailureDecision.Rethrow

    fun isRetryable(error: Throwable): Boolean = failureDecision(error, 0) is FailureDecision.Retry

    private fun SQLException.isTransientDatabaseFailure(): Boolean =
        this is SQLTransientException || this is SQLRecoverableException
}
