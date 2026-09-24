package f95gm.server.state.runtime

internal class ServerLifecycleCloseException(failures: List<Exception>) :
    IllegalStateException("Failed to close ${failures.size} server resource(s)") {
    init {
        failures.drop(1).forEach(::addSuppressed)
        failures.firstOrNull()?.let(::initCause)
    }
}
