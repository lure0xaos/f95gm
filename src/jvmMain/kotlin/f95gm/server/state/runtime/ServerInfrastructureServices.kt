package f95gm.server.state.runtime

import f95gm.server.kernel.ServerClock

import f95gm.server.http.request.RequestMetrics
import f95gm.server.application.SessionRegistry

internal interface RequestAdmissionInfrastructure {
    val clock: ServerClock
    val requestMetrics: RequestMetrics
    val sessions: SessionRegistry
}

internal interface RequestMonitoringInfrastructure : RequestAdmissionInfrastructure {
    val activeRequests: ActiveRequestTracker
}

/** Monitoring-only infrastructure slice exposed to request admission and telemetry. */
internal data class RequestMonitoringInfrastructureServices(
    override val clock: ServerClock,
    override val requestMetrics: RequestMetrics,
    override val sessions: SessionRegistry,
    override val activeRequests: ActiveRequestTracker
) : RequestMonitoringInfrastructure
