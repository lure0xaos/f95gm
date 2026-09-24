package f95gm.server.state.runtime

import f95gm.server.config.ServerConfig
import f95gm.server.persistence.ServerRepositories
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.upstream.request.UpstreamGateway
import io.ktor.client.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class ServerContext(
    val config: ServerConfig,
    override val clock: ServerClock = SystemServerClock,
    repositories: ServerRepositories? = null,
    httpClient: HttpClient? = null,
    databaseExecutor: DatabaseExecutor,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AutoCloseable, RouteDependencies {
    private val services = ServerContextServices(config, clock, databaseExecutor, repositories, httpClient, dispatcher)
    val requestMetrics get() = services.requestMetrics
    override val sessions get() = services.sessions
    val activeRequests get() = services.activeRequests
    val staticCache get() = services.staticCache
    override val publicHostResolver get() = services.publicHostResolver
    override val mediaFetcher get() = services.mediaFetcher
    override val liveMarkCache get() = services.liveMarkCache
    val idempotency get() = services.idempotency
    val mutationService get() = services.mutationService
    override val upstream: UpstreamGateway
        get() {
            check(!services.isClosed()) { "Server context is closed" }
            return services.upstream
        }
    override val sessionService get() = services.sessionService
    override val applicationServices: RouteApplicationServices get() = services.applicationServices

    override fun close() {
        services.close()
    }
}
