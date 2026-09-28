package f95gm.server.state.runtime

import f95gm.server.kernel.ShutdownFailureTarget
import f95gm.server.kernel.TerminalFailureCoordinator
import f95gm.server.kernel.LifecycleResourceName
import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.LifecycleState

internal inline fun rethrowAfterFailure(
    error: Throwable,
    publish: (Throwable) -> Unit
): Nothing {
    publish(error)
    throw error
}

/** Runs a terminal lifecycle operation and publishes both ordinary and fatal failures consistently. */
@Suppress("TooGenericExceptionCaught") // This helper is the single terminal lifecycle exception boundary.
internal inline fun <T> runTerminalBoundary(
    operation: ShutdownFailureTarget,
    block: () -> T
): T = try {
    block()
} catch (failure: Exception) {
    TerminalFailureCoordinator(operation).fail(failure)
} catch (failure: Error) {
    TerminalFailureCoordinator(operation).fail(failure)
}

/** Suspend-aware counterpart used by shutdown paths that must preserve cancellation. */
@Suppress("TooGenericExceptionCaught")
internal suspend inline fun <T> runSuspendTerminalBoundary(
    operation: ShutdownFailureTarget,
    crossinline block: suspend () -> T
): T = try {
    block()
} catch (cancelled: kotlinx.coroutines.CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    TerminalFailureCoordinator(operation).fail(failure)
} catch (failure: Error) {
    TerminalFailureCoordinator(operation).fail(failure)
}

/** Isolates ordinary cleanup failures while preserving coroutine cancellation. */
@Suppress("TooGenericExceptionCaught")
internal suspend inline fun runSuspendCleanupBoundary(
    crossinline onFailure: (Exception) -> Unit,
    crossinline block: suspend () -> Unit
) {
    try {
        block()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onFailure(failure)
    }
}

/** Synchronous counterpart used by AutoCloseable cleanup paths. */
@Suppress("TooGenericExceptionCaught")
internal inline fun runCleanupBoundary(
    crossinline onFailure: (Exception) -> Unit,
    crossinline block: () -> Unit
) {
    try {
        block()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onFailure(failure)
    }
}

/** Executes one synchronous cleanup action and records ordinary failures consistently. */
@Suppress("TooGenericExceptionCaught") // Cleanup must aggregate ordinary failures while preserving cancellation.
internal inline fun collectCleanupFailure(
    failures: CleanupFailureCollector,
    crossinline action: () -> Unit
) = runCleanupBoundary(failures::add, action)

/** Collects ordinary cleanup failures while leaving cancellation and fatal errors to the caller. */
internal class CleanupFailureCollector {
    private val failures = mutableListOf<CleanupFailure>()

    val isNotEmpty: Boolean get() = failures.isNotEmpty()
    fun summary(): CleanupFailureSummary = CleanupFailureSummary(failures.toList())

    fun add(failure: Exception) = addInternal(null, null, null, CleanupFailureCategory.CLEANUP, failure)
    fun add(resource: LifecycleResourceName, failure: Exception) =
        addInternal(resource, null, null, CleanupFailureCategory.CLEANUP, failure)
    fun add(
        resource: LifecycleResourceName,
        boundary: FailureBoundary,
        phase: LifecycleState,
        category: CleanupFailureCategory,
        failure: Exception
    ) = addInternal(resource, boundary, phase, category, failure)
    private fun addInternal(
        resource: LifecycleResourceName?,
        boundary: FailureBoundary?,
        phase: LifecycleState?,
        category: CleanupFailureCategory,
        failure: Exception
    ) {
        failures += CleanupFailure(resource, boundary, phase, category, failure)
    }

}

/** Immutable result of a best-effort cleanup pass. */
internal data class CleanupFailureSummary(val failures: List<CleanupFailure>) {
    init { require(failures.isNotEmpty()) }
    val primary: Exception get() = failures.first().error
    val count: Int get() = failures.size
    val resources: List<LifecycleResourceName> get() = failures.mapNotNull { it.resource }

    fun addRemainingAsSuppressed(target: Throwable) {
        failures.drop(1).forEach { target.addSuppressed(it.error) }
    }
}

internal data class CleanupFailure(
    val resource: LifecycleResourceName?,
    val boundary: FailureBoundary?,
    val phase: LifecycleState?,
    val category: CleanupFailureCategory,
    val error: Exception
)

internal enum class CleanupFailureCategory {
    CLEANUP,
    RESOURCE_CLOSE,
    TIMEOUT,
    PUBLICATION
}
