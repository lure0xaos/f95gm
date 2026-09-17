package f95gm.server.model

internal sealed interface ConditionalWriteResult<out T> {
    data class Stored<T>(val value: T) : ConditionalWriteResult<T>
    data class Conflict(val expectedUpdatedAt: EpochMillis?, val actualUpdatedAt: EpochMillis?) : ConditionalWriteResult<Nothing>
}
