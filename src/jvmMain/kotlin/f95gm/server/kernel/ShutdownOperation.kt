package f95gm.server.kernel

import java.util.concurrent.atomic.AtomicReference

internal enum class ShutdownPhase { OPEN, CLOSING, AWAITING_JOBS, CLOSING_CLIENT, COMPLETED, FAILED }

/** Minimal failure-publication capability used by lifecycle exception boundaries. */
internal interface ShutdownFailureTarget {
    fun fail(error: Throwable): Nothing
}

/** Coordinates one idempotent shutdown owner with any number of awaiters. */
internal class ShutdownOperation : ShutdownFailureTarget {
    private val completion = CompletionSignal()
    private val phaseState = ValidatedState(ShutdownPhase.OPEN, allowedTransitions)
    private val terminalFailure = AtomicReference<Throwable?>(null)

    val isStarted: Boolean get() = phaseState.current() != ShutdownPhase.OPEN
    val phase: ShutdownPhase get() = phaseState.current()
    val failure: Throwable? get() = terminalFailure.get()

    fun tryStart(): Boolean = tryTransition(ShutdownPhase.CLOSING)

    fun transition(next: ShutdownPhase) = phaseState.transition(next)

    suspend fun await() = completion.await()

    fun complete() {
        phaseState.transition(ShutdownPhase.COMPLETED, allowRepeated = true)
        completion.complete()
    }

    override fun fail(error: Throwable): Nothing {
        val first = terminalFailure.updateAndGet { it ?: error } ?: error
        if (phaseState.tryTransition(ShutdownPhase.FAILED)) completion.fail(first)
        throw first
    }

    private fun tryTransition(next: ShutdownPhase): Boolean = phaseState.tryTransition(next)

    private companion object {
        val allowedTransitions = mapOf(
            ShutdownPhase.OPEN to setOf(ShutdownPhase.CLOSING, ShutdownPhase.COMPLETED, ShutdownPhase.FAILED),
            ShutdownPhase.CLOSING to setOf(ShutdownPhase.AWAITING_JOBS, ShutdownPhase.CLOSING_CLIENT, ShutdownPhase.COMPLETED, ShutdownPhase.FAILED),
            ShutdownPhase.AWAITING_JOBS to setOf(ShutdownPhase.CLOSING_CLIENT, ShutdownPhase.FAILED),
            ShutdownPhase.CLOSING_CLIENT to setOf(ShutdownPhase.COMPLETED, ShutdownPhase.FAILED),
            ShutdownPhase.COMPLETED to emptySet(),
            ShutdownPhase.FAILED to emptySet()
        )
    }
}
