package f95gm.server.kernel

import java.util.concurrent.atomic.AtomicReference

/** Shared open/closing/completed lifecycle state machine for owned resources. */
internal class LifecycleGate {
    private val state = ValidatedState(LifecycleState.OPEN, allowedTransitions)
    private val terminalFailure = AtomicReference<Throwable?>(null)

    val isOpen: Boolean get() = state.current() == LifecycleState.OPEN
    val isClosing: Boolean get() = state.current() == LifecycleState.CLOSING
    val isClosed: Boolean get() = !isOpen
    val currentState: LifecycleState get() = state.current()
    val failure: Throwable? get() = terminalFailure.get()

    fun beginClose(): Boolean = state.tryTransition(LifecycleState.CLOSING)

    fun complete(): Boolean = state.tryTransition(LifecycleState.COMPLETED)

    fun fail(cause: Throwable? = null): Boolean = state.tryTransition(LifecycleState.FAILED).also {
        if (it && cause != null) terminalFailure.compareAndSet(null, cause)
    }

    private companion object {
        val allowedTransitions = mapOf(
            LifecycleState.OPEN to setOf(LifecycleState.CLOSING, LifecycleState.FAILED),
            LifecycleState.CLOSING to setOf(LifecycleState.COMPLETED, LifecycleState.FAILED),
            LifecycleState.COMPLETED to emptySet(),
            LifecycleState.FAILED to emptySet()
        )
    }
}
