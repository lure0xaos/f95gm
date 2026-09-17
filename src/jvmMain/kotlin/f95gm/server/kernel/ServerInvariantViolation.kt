package f95gm.server.kernel

import f95gm.server.kernel.BoundaryFailure
import f95gm.server.kernel.FailureCategory

/** Signals an impossible internal state, distinct from invalid client input. */
internal open class ServerInvariantViolation(message: String, cause: Throwable? = null) :
    BoundaryFailure(message, cause, FailureCategory.LIFECYCLE)

/** Structured failure emitted when lifecycle shutdown cannot finish within its budget. */
internal class ShutdownFailure(
    val resource: LifecycleResourceName,
    val phase: ShutdownPhase,
    val activeWork: Int?,
    cause: Throwable
) : ServerInvariantViolation(
    "Timed out waiting for ${resource.value} to shut down (phase=$phase, activeWork=${activeWork ?: "unknown"})",
    cause
)

internal fun shutdownTimeoutViolation(
    resource: String,
    cause: Throwable,
    phase: ShutdownPhase = ShutdownPhase.AWAITING_JOBS,
    activeWork: Int? = null
): ShutdownFailure = ShutdownFailure(LifecycleResourceName(resource), phase, activeWork, cause)
