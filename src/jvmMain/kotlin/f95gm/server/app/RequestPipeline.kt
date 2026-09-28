package f95gm.server.app

import f95gm.server.http.request.RequestEndpoint
import f95gm.server.http.request.RequestId
import f95gm.server.http.request.RequestMetrics
import f95gm.server.state.runtime.ActiveRequestTracker
import f95gm.server.kernel.ServerClock
import io.ktor.server.application.ApplicationCall

internal fun interface RequestPipelineStage {
    suspend fun invoke(call: ApplicationCall, state: RequestPipelineState, next: suspend () -> Unit)
}

internal data class RequestPipelineConfiguration(
    val stages: List<RequestPipelineStage> = emptyList()
) {
    internal companion object {
        val DEFAULT = RequestPipelineConfiguration()
    }
}

/** Owns the lifecycle bookkeeping shared by every monitored request. */
internal class RequestPipeline(
    private val clock: ServerClock,
    private val metrics: RequestMetrics,
    private val activeRequests: ActiveRequestTracker,
    configuration: RequestPipelineConfiguration = RequestPipelineConfiguration.DEFAULT
) {
    private val stages: List<RequestPipelineStage> = configuration.stages.toList()
    fun start(requestId: RequestId): RequestPipelineState = RequestPipelineState(
        requestId = requestId,
        started = clock.snapshot(),
        unmatchedEndpoint = RequestEndpoint(RequestMetricEndpoint.UNMATCHED.wireName)
    )

    fun finish(call: ApplicationCall, state: RequestPipelineState) {
        if (state.phase != RequestLifecyclePhase.COMPLETED) state.complete()
        recordRequestCompletion(
            call = call,
            requestId = state.requestId,
            started = state.started,
            endpoint = state.metricEndpoint(),
            metrics = metrics,
            failure = state.failure,
            now = clock::snapshot
        )
    }

    suspend fun enter() = activeRequests.enter()

    suspend fun execute(call: ApplicationCall, state: RequestPipelineState, terminal: suspend () -> Unit) {
        state.beginExecution()
        suspend fun invokeAt(index: Int) {
            if (index == stages.size) terminal()
            else stages[index].invoke(call, state) { invokeAt(index + 1) }
        }
        invokeAt(0)
    }
}
