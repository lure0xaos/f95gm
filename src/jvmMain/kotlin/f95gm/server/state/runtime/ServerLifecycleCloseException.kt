package f95gm.server.state.runtime

import f95gm.server.kernel.ServerInvariantViolation

internal class ServerLifecycleCloseException(summary: CleanupFailureSummary) :
    ServerInvariantViolation(
        "Failed to close ${summary.count} server resource(s): ${summary.resources.joinToString { it.value }} " +
            "(${summary.failures.joinToString { it.category.name }})",
        summary.primary
    ) {
        val failures: List<CleanupFailure> = summary.failures
        init {
            summary.addRemainingAsSuppressed(this)
        }
    }
