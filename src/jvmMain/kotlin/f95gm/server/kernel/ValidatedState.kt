package f95gm.server.kernel

/** Lock-protected finite-state value with one authoritative transition table. */
internal class ValidatedState<S>(
    initial: S,
    private val allowedTransitions: Map<S, Set<S>>
) {
    private val lock = ResourceLock()
    private var value = initial

    fun current(): S = lock.withLock { value }

    fun transition(next: S, allowRepeated: Boolean = false): Boolean = lock.withLock {
        if (allowRepeated && next == value) return@withLock false
        check(next in allowedTransitions[value].orEmpty()) { "Invalid state transition: $value -> $next" }
        value = next
        true
    }

    /** Attempts a legal transition without converting invariant violations into exceptions. */
    fun tryTransition(next: S, allowRepeated: Boolean = false): Boolean = lock.withLock {
        if (allowRepeated && next == value) return@withLock false
        if (next !in allowedTransitions[value].orEmpty()) return@withLock false
        value = next
        true
    }
}
