package f95gm.server.state.runtime

import f95gm.server.kernel.ShutdownCoordinator

/** Lifecycle-owned facade; runtime code does not depend on the kernel shutdown primitive. */
internal class LifecycleShutdownCoordinator {
    private val operation = ShutdownCoordinator()

    val isStarted: Boolean get() = operation.isStarted
    val failure: Throwable? get() = operation.failure
    suspend fun await() = operation.await()
    fun tryStart(): Boolean = operation.tryStart()
    fun complete() = operation.complete()
    fun fail(error: Throwable): Nothing = operation.fail(error)

    inline fun <T> runTerminal(block: () -> T): T = runTerminalBoundary(operation, block)
    suspend inline fun <T> runSuspendTerminal(crossinline block: suspend () -> T): T =
        runSuspendTerminalBoundary(operation, block)
}
