package f95gm.server.state.runtime

import f95gm.server.kernel.ServerClock

import f95gm.server.config.ServerConfig
import f95gm.server.http.request.RequestMetrics
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.application.SessionRegistry
import f95gm.server.upstream.request.UpstreamHttpClient
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope

internal data class ServerUpstreamRuntimePolicy(
    val request: f95gm.server.upstream.request.UpstreamPolicy,
    val circuitBreakerOpen: Duration,
    val maxCircuitBreakerEntries: Int,
    val maxConcurrentRequests: Int = 32
)

internal data class ServerUpstreamDependencies(
    val client: UpstreamHttpClient,
    val scope: CoroutineScope,
    val config: ServerConfig,
    val policy: ServerUpstreamRuntimePolicy,
    val clock: ServerClock,
    val sessions: SessionRegistry,
    val database: TransactionBoundary,
    val metrics: RequestMetrics
)
