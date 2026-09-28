package f95gm.server.state.runtime

import f95gm.server.kernel.FailureBoundary
import f95gm.server.observability.FailureReporter
import f95gm.server.observability.FailureDescriptor
import f95gm.server.observability.FailureScopes
import f95gm.server.kernel.failureCategory
import f95gm.server.observability.logger
import f95gm.server.kernel.LifecycleResourceName

/** Single reporting policy for failures observed while closing owned resources. */
internal object LifecycleFailurePolicy {
    fun report(resource: LifecycleResourceName, boundary: FailureBoundary, failure: Throwable, message: String) {
        FailureReporter.report(FailureScopes.resource(resource), FailureDescriptor(boundary, failure.failureCategory()), failure)
        logger.warn(failure) { message }
    }

}
