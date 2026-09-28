package f95gm.server.kernel

/** Public kernel boundary for shutdown coordination; the state machine stays private. */
internal class ShutdownCoordinator : ShutdownFailureTarget {
    private val operation = ShutdownOperation()

    val isStarted: Boolean get() = operation.isStarted
    val phase: ShutdownPhase get() = operation.phase
    val failure: Throwable? get() = operation.failure
    fun tryStart(): Boolean = operation.tryStart()
    fun transition(next: ShutdownPhase) = operation.transition(next)
    suspend fun await() = operation.await()
    fun complete() = operation.complete()
    override fun fail(error: Throwable): Nothing = operation.fail(error)
}
