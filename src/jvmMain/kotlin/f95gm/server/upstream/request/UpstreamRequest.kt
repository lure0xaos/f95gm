@file:Suppress("MatchingDeclarationName") // This file groups the upstream client around the request contract.

package f95gm.server.upstream.request

import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.server.observability.FailureReporter
import f95gm.server.observability.FailureScopes
import f95gm.server.observability.FailureDescriptor
import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.failureCategory
import f95gm.server.observability.UpstreamMetrics
import f95gm.server.http.request.RequestMetrics
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.kernel.ServerClock
import f95gm.domain.api.UpstreamMetricsSnapshotContract
import f95gm.domain.api.RetryJitter
import f95gm.server.kernel.SingleFlight
import f95gm.server.platform.SystemServerClock
import f95gm.server.state.runtime.session.SessionLockRegistry
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class UpstreamClient(
    httpClient: UpstreamHttpClient,
    scope: CoroutineScope,
    private val sessionLocks: SessionLockRegistry = SessionLockRegistry(),
    private val circuitBreaker: UpstreamCircuitBreaker = UpstreamCircuitBreaker(SystemServerClock),
    private val policy: UpstreamPolicy = UpstreamPolicy(),
    override val endpointConfig: UpstreamEndpointConfig,
    private val clock: ServerClock = SystemServerClock,
    random: Random = Random.Default,
    retryJitter: RetryJitter = RetryJitter { _, _ -> random.nextLong(0, 101).milliseconds },
    retryDelay: suspend (Duration) -> Unit = { delay(it) },
    metrics: RequestMetrics = RequestMetrics(),
    private val upstreamMetrics: UpstreamMetrics = metrics.upstreamMetrics(),
    maxConcurrentRequests: Int = UpstreamConcurrencyDispatcher.DEFAULT_MAX_CONCURRENT_REQUESTS,
    persistSession: suspend (UpstreamSession) -> Unit = {}
) : UpstreamGateway, LifecycleResource {
    private val closeGuard = CloseGuard()
    private val getFlights = SingleFlight<UpstreamFlightKey, UpstreamResponse>(scope) { _, failure ->
        FailureReporter.report(
            FailureScopes.singleFlight(),
            FailureDescriptor(FailureBoundary.UPSTREAM, failure.failureCategory()),
            failure
        )
    }
    private val responseReader = UpstreamResponseReader(persistSession = persistSession)
    private val transport = UpstreamTransport(httpClient, clock, retryJitter, retryDelay, metrics, endpointConfig.baseUrl)
    private val coordinator = UpstreamRequestCoordinator(
        transport,
        responseReader,
        sessionLocks,
        getFlights,
        circuitBreaker,
        policy,
        clock,
        upstreamMetrics,
        maxConcurrentRequests
    )

    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce {
        getFlights.close()
        sessionLocks.close()
        circuitBreaker.close()
    }

    fun upstreamMetricsSnapshot(): UpstreamMetricsSnapshotContract = upstreamMetrics.snapshot()

    override suspend fun request(request: UpstreamRequest): UpstreamResponse = coordinator.request(
        request.method, request.path, request.context()
    )

}

