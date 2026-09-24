package f95gm.server.state.runtime

import f95gm.server.config.ServerConfig
import f95gm.server.http.request.MutationService
import f95gm.server.http.request.RequestMetrics
import f95gm.server.http.static.StaticResourceCache
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.persistence.ServerRepositories
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.routes.item.MediaFetcher
import f95gm.server.routes.item.PinnedMediaFetcher
import f95gm.server.routes.item.PublicHostResolver
import f95gm.server.state.runtime.cache.BoundedTtlCache
import f95gm.server.state.runtime.session.SessionRegistry
import f95gm.server.upstream.request.UpstreamGateway
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

/** Owns runtime services so ServerContext remains a route-facing dependency facade. */
internal class ServerContextServices(
    private val config: ServerConfig,
    val clock: ServerClock,
    databaseExecutor: DatabaseExecutor,
    repositories: ServerRepositories?,
    httpClient: HttpClient?,
    dispatcher: CoroutineDispatcher
) : AutoCloseable {
    val requestMetrics = RequestMetrics()
    private val httpPolicies = ServerHttpPolicies.from(config)
    private val resources = ServerResources(httpPolicies.clientTimeouts, dispatcher, httpClient)
    val sessions = SessionRegistry(clock, config.limits.maxSessions)
    val activeRequests = ActiveRequestTracker()
    val staticCache = StaticResourceCache(config.staticDirectory)
    val publicHostResolver = PublicHostResolver(clock)
    val mediaFetcher: MediaFetcher = PinnedMediaFetcher(
        publicHostResolver,
        config.timeouts.connectMillis.value.toInt(),
        config.timeouts.socketMillis.value.toInt()
    )
    val liveMarkCache = BoundedTtlCache.withTtl<String, LiveMarkInfo>(1_024, 30.seconds, clock)
    val idempotency = IdempotencyRegistry(clock = clock)
    val mutationService = MutationService(idempotency)
    private val persistence =
        ServerPersistenceServices(databaseExecutor, sessions, clock, mutationService, repositories)
    private val upstreamServices = ServerUpstreamServices(
        ServerUpstreamDependencies(
            resources.upstreamHttpClient,
            resources.scope,
            config,
            httpPolicies.upstream,
            clock,
            sessions,
            databaseExecutor,
            requestMetrics
        )
    )
    val upstream: UpstreamGateway get() = upstreamServices.gateway
    val sessionService get() = persistence.sessionService
    val applicationServices get() = persistence.applicationServices
    private val lifecycle = ServerLifecycle()
    private val sessionEvictionJob: Job

    init {
        lifecycle.register(publicHostResolver)
        lifecycle.register(upstreamServices)
        lifecycle.register(staticCache)
        lifecycle.register(resources)
        val interval = config.timeouts.sessionIdleMillis.value.coerceAtMost(60_000L)
        sessionEvictionJob = resources.scope.launch {
            while (isActive) {
                delay(interval)
                sessions.evictIdle(clock.nowMillis(), config.timeouts.sessionIdleMillis.value)
            }
        }
        lifecycle.register(AutoCloseable { sessionEvictionJob.cancel() })
    }

    val isClosed: Boolean get() = lifecycle.isClosed
    override fun close() = lifecycle.close()
}
