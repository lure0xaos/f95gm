package f95gm.client.data.api

import f95gm.client.actions.connection.recordJvmUnavailable
import f95gm.client.actions.observability.recordApiResponse
import f95gm.client.state.runtime.ClientApiEventPort
import f95gm.client.state.runtime.reduce
import kotlinx.coroutines.launch

/** Side effects emitted by the transport boundary, injectable for tests and alternate runtimes. */
internal interface ApiClientEvents {
    fun emit(event: ApiTelemetryEvent)
}

internal fun interface ApiTelemetrySink {
    fun record(event: ApiTelemetryEvent)
}

internal class RuntimeApiTelemetrySink(private val runtime: ClientApiEventPort) : ApiTelemetrySink {
    override fun record(event: ApiTelemetryEvent) {
        runtime.launchEffect {
            runtime.apiTelemetry.enqueue { current -> current.reduce(event) }
        }
    }
}

internal object NoopApiClientEvents : ApiClientEvents {
    override fun emit(event: ApiTelemetryEvent) = Unit
}

internal class RuntimeApiClientEvents(
    private val runtime: ClientApiEventPort,
    private val telemetrySink: ApiTelemetrySink = RuntimeApiTelemetrySink(runtime)
) : ApiClientEvents {
    override fun emit(event: ApiTelemetryEvent) {
        when (event) {
            is ApiTelemetryEvent.Response -> {
                recordApiResponse(runtime, event.path, event.status)
                telemetrySink.record(event)
            }
            is ApiTelemetryEvent.Completed,
            is ApiTelemetryEvent.Failure -> telemetrySink.record(event)
            is ApiTelemetryEvent.Unavailable -> {
                recordJvmUnavailable(runtime)
                telemetrySink.record(event)
            }
        }
    }
}
