package f95gm.server.state.runtime

import f95gm.server.kernel.ShutdownCoordinator
import f95gm.server.kernel.ShutdownPhase

/** Resource-specific facade over shutdown phases and completion. */
internal class ResourceShutdownCoordinator {
    private val operation = ShutdownCoordinator()
    val phase: ShutdownPhase get() = operation.phase
    fun tryStart(): Boolean = operation.tryStart()
    suspend fun await() = operation.await()
    fun beginWaitingForJobs() = operation.transition(ShutdownPhase.AWAITING_JOBS)
    fun beginClientClose() = operation.transition(ShutdownPhase.CLOSING_CLIENT)
    fun complete() = operation.complete()
    fun fail(error: Throwable): Nothing = operation.fail(error)

    inline fun <T> runTerminal(block: () -> T): T = runTerminalBoundary(operation, block)
    suspend inline fun <T> runSuspendTerminal(crossinline block: suspend () -> T): T =
        runSuspendTerminalBoundary(operation, block)
}
