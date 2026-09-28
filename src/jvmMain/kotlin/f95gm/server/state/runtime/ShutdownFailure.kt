package f95gm.server.state.runtime

import f95gm.server.kernel.ShutdownFailureTarget
import f95gm.server.kernel.TerminalFailureCoordinator

/** Publishes terminal shutdown failures through the shared operation. */
internal fun publishShutdownFailure(operation: ShutdownFailureTarget, failure: Exception) {
    TerminalFailureCoordinator(operation).fail(failure)
}

/** Publishes fatal shutdown failures without swallowing the fatal error. */
internal fun publishShutdownFailure(operation: ShutdownFailureTarget, failure: Error) {
    TerminalFailureCoordinator(operation).fail(failure)
}
