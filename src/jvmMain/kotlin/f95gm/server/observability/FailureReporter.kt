package f95gm.server.observability

import f95gm.server.kernel.failureCategory
import f95gm.server.kernel.FailureCategory
import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.LifecycleResourceName
import kotlinx.coroutines.CancellationException

/** Centralizes structured exception reporting while preserving the original throwable for propagation. */
internal object FailureReporter {
    fun report(scope: FailureScope, descriptor: FailureDescriptor, error: Throwable) {
        logger.warn {
            structuredLog(
                FailureReportedEvent(scope, descriptor.category, ExceptionType(error::class.simpleName ?: "Throwable"))
            )
        }
    }

    inline fun <T> reportAndRethrow(
        scope: FailureScope,
        boundary: FailureBoundary,
        error: Throwable,
        cleanup: () -> Unit = {}
    ): T {
        report(scope, FailureDescriptor(boundary, error.failureCategory()), error)
        cleanup()
        throw error
    }

    /** Shared infrastructure boundary that preserves cancellation and normalizes ordinary failures. */
    @Suppress("TooGenericExceptionCaught") // This helper is the shared infrastructure exception boundary.
    inline fun <T> runBoundary(
        scope: FailureScope,
        boundary: FailureBoundary,
        crossinline transform: (Exception) -> Throwable,
        crossinline onFailure: (Exception) -> Unit = {},
        block: () -> T
    ): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        report(scope, FailureDescriptor(boundary, failure.failureCategory()), failure)
        onFailure(failure)
        throw transform(failure)
    }

    /** Application boundary that records ordinary failures while leaving cancellation untouched. */
    @Suppress("TooGenericExceptionCaught") // Application callers receive the original ordinary failure unchanged.
    suspend inline fun <T> runApplicationBoundary(
        crossinline onFailure: (Exception) -> Unit,
        block: suspend () -> T
    ): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onFailure(failure)
        throw failure
    }

}

internal data class FailureDescriptor(
    val boundary: FailureBoundary,
    val category: FailureCategory
)

@JvmInline
internal value class FailureScope(val value: String) {
    init { require(value.isNotBlank()) }
}

/** Centralizes construction of dynamic scopes so their wire format remains stable. */
internal object FailureScopes {
    fun cache(namespace: CacheFailureNamespace): FailureScope = FailureScope("cache:${namespace.value}")
    fun upstream(key: UpstreamFailureKey): FailureScope = FailureScope("upstream:${key.value}")
    fun resource(name: LifecycleResourceName): FailureScope = FailureScope(name.value)
    fun singleFlight(): FailureScope = FailureScope("single-flight")
}

@JvmInline
internal value class CacheFailureNamespace(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
internal value class UpstreamFailureKey(val value: String) {
    init { require(value.isNotBlank()) }
}
