package f95gm.server.persistence

internal sealed interface ConditionalWriteResult<out T> {
    data class Stored<T>(val value: T) : ConditionalWriteResult<T>
    data object Conflict : ConditionalWriteResult<Nothing>
}
