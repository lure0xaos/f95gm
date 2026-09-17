package f95gm.server.kernel

/** Shares one close transition and one terminal result between synchronous and suspending callers. */
internal class TerminalCloseCoordinator {
    private val lifecycle = LifecycleGate()
    private val completion = CompletionSignal()

    val isOpen: Boolean get() = lifecycle.isOpen
    val isClosed: Boolean get() = lifecycle.isClosed
    val failure: Throwable? get() = lifecycle.failure

    fun beginClose(): Boolean = lifecycle.beginClose()
    fun complete() {
        lifecycle.complete()
        completion.complete()
    }
    fun fail(error: Throwable): Nothing {
        lifecycle.fail(error)
        completion.fail(error)
        throw error
    }
    suspend fun await() = completion.await()
}
