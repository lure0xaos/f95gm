package f95gm.server.app

import f95gm.server.desktop.DesktopLifecycle
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.kernel.ServerInvariantViolation
import f95gm.server.state.runtime.CleanupFailureCollector
import f95gm.server.state.runtime.collectCleanupFailure
import io.ktor.server.engine.ApplicationEngine
import f95gm.server.kernel.ResourceLock

internal enum class ServerRuntimePhase { NEW, SERVER_BOUND, RUNNING, CLOSED }

internal enum class RuntimeLifecycleState { NEW, SERVER_BOUND, RUNNING, CLOSED }

internal fun RuntimeLifecycleState.toPublicPhase(): ServerRuntimePhase = when (this) {
    RuntimeLifecycleState.NEW -> ServerRuntimePhase.NEW
    RuntimeLifecycleState.SERVER_BOUND -> ServerRuntimePhase.SERVER_BOUND
    RuntimeLifecycleState.RUNNING -> ServerRuntimePhase.RUNNING
    RuntimeLifecycleState.CLOSED -> ServerRuntimePhase.CLOSED
}

internal object RuntimeShutdownPlan {
    fun close(
        desktop: DesktopLifecycle?,
        shutdownController: ServerShutdownController?,
        server: ApplicationEngine?
    ) {
        val failures = CleanupFailureCollector()
        attempt(failures) { desktop?.dispose() }
        attempt(failures) { shutdownController?.close() }
        attempt(failures) { server?.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000) }
        if (failures.isNotEmpty) {
            val summary = failures.summary()
            val failure = ServerRuntimeCloseFailure(summary.primary)
            summary.addRemainingAsSuppressed(failure)
            throw failure
        }
    }

    private inline fun attempt(failures: CleanupFailureCollector, crossinline action: () -> Unit) {
        collectCleanupFailure(failures, action)
    }
}

private class ServerRuntimeCloseFailure(cause: Throwable) :
    ServerInvariantViolation("Failed to close one or more server runtime resources", cause)

internal class LifecycleSlot<T>(initial: T? = null) {
    private sealed interface State<out T> {
        data object Unbound : State<Nothing>
        data class Bound<T>(val value: T) : State<T>
        data object Taken : State<Nothing>
    }

    private val lock = ResourceLock()
    private var value: State<T> = initial?.let(State<T>::Bound) ?: State.Unbound
    fun bind(next: T) = lock.withLock {
        check(value == State.Unbound) { "Lifecycle resource is already bound" }
        value = State.Bound(next)
    }
    fun replace(next: T) = lock.withLock {
        check(value is State.Bound) { "Lifecycle resource is not bound" }
        value = State.Bound(next)
    }
    fun get(): T = lock.withLock {
        (value as? State.Bound<T>)?.value
            ?: throw ServerInvariantViolation("Lifecycle resource is not bound")
    }
    fun peek(): T? = lock.withLock { (value as? State.Bound<T>)?.value }
    fun take(): T? = lock.withLock {
        val previous = value
        value = State.Taken
        (previous as? State.Bound<T>)?.value
    }
}
