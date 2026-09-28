package f95gm.server.kernel

import kotlin.time.Duration

/** Dependency-free result contract shared by retrying infrastructure. */
internal sealed interface RetryDecision<out T> {
    data class Return<T>(val value: T) : RetryDecision<T>
    data class Again(val delay: Duration) : RetryDecision<Nothing>
}
