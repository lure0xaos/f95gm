package f95gm.server.upstream.request

import f95gm.server.http.request.RequestMetrics
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.state.runtime.*
import f95gm.server.state.runtime.session.SessionLockRegistry
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlin.random.Random

internal class UpstreamClient(
    httpClient: UpstreamHttpClient,
    scope: CoroutineScope,
    private val sessionLocks: SessionLockRegistry = SessionLockRegistry(),
    private val circuitBreaker: UpstreamCircuitBreaker = UpstreamCircuitBreaker(SystemServerClock),
    private val policy: UpstreamPolicy = UpstreamPolicy(),
    private val clock: ServerClock = SystemServerClock,
    random: Random = Random.Default,
    retryJitterMillis: (Int) -> Long = { random.nextLong(0, 101) },
    retryDelay: suspend (Long) -> Unit = { delay(it) },
    metrics: RequestMetrics = RequestMetrics(),
    persistSession: suspend (UpstreamSession) -> Unit = {}
) : UpstreamGateway, AutoCloseable {
    private val getFlights = SingleFlight<String, UpstreamResponse>(scope)
    private val responseReader = UpstreamResponseReader(policy.maxResponseBytes, persistSession)
    private val retryPolicy = UpstreamRetryPolicy(policy, clock, retryJitterMillis)
    private val transport = UpstreamTransport(httpClient, policy, retryPolicy, retryDelay, metrics)
    private val coordinator = UpstreamRequestCoordinator(
        transport,
        responseReader,
        sessionLocks,
        getFlights,
        circuitBreaker,
        policy,
        clock
    )

    override fun close() {
        getFlights.close()
        sessionLocks.close()
        circuitBreaker.close()
    }

    override suspend fun request(
        method: HttpMethod,
        path: String,
        session: UpstreamSession,
        body: String?,
        contentType: String?,
        headers: UpstreamHeaders
    ): UpstreamResponse = coordinator.request(method, path, session, body, contentType, headers.values)

    suspend fun request(method: HttpMethod, path: String, session: UpstreamSession): UpstreamResponse =
        request(method, path, session, null, null, UpstreamHeaders.EMPTY)
}

internal suspend fun ServerContext.upstreamRequest(
    method: HttpMethod,
    path: String,
    session: UpstreamSession,
    body: String? = null,
    contentType: String? = null,
    headers: UpstreamHeaders = UpstreamHeaders.EMPTY
): UpstreamResponse = upstream.request(method, path, session, body, contentType, headers)

internal suspend fun RouteDependencies.upstreamRequest(
    method: HttpMethod,
    path: String,
    session: UpstreamSession,
    body: String? = null,
    contentType: String? = null,
    headers: UpstreamHeaders = UpstreamHeaders.EMPTY
): UpstreamResponse = upstream.request(method, path, session, body, contentType, headers)
