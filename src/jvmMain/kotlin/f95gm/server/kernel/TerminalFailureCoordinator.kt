package f95gm.server.kernel

/** Publishes one terminal failure to all shutdown awaiters and preserves propagation. */
internal class TerminalFailureCoordinator(private val operation: ShutdownFailureTarget) {
    fun fail(error: Throwable): Nothing {
        operation.fail(error)
        throw error
    }
}
