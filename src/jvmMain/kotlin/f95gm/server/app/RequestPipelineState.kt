package f95gm.server.app

import f95gm.server.http.request.RequestEndpoint
import f95gm.server.http.request.RequestId
import f95gm.server.kernel.ClockSnapshot

/** Mutable state owned by one monitored request from admission through completion. */
internal enum class RequestLifecyclePhase { CREATED, EXECUTING, FAILED, COMPLETED }

internal class RequestPipelineState(
    val requestId: RequestId,
    val started: ClockSnapshot,
    private val unmatchedEndpoint: RequestEndpoint
) {
    var endpoint: RequestEndpoint = unmatchedEndpoint
    var failure: RequestFailure? = null
    var phase: RequestLifecyclePhase = RequestLifecyclePhase.CREATED
        private set

    fun beginExecution() {
        check(phase == RequestLifecyclePhase.CREATED) { "Request pipeline cannot start from $phase" }
        phase = RequestLifecyclePhase.EXECUTING
    }

    fun recordFailure(value: RequestFailure) {
        failure = value
        phase = RequestLifecyclePhase.FAILED
    }

    fun complete() {
        check(phase == RequestLifecyclePhase.CREATED || phase == RequestLifecyclePhase.EXECUTING || phase == RequestLifecyclePhase.FAILED) {
            "Request pipeline cannot complete from $phase"
        }
        phase = RequestLifecyclePhase.COMPLETED
    }
}
