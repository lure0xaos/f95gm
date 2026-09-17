package f95gm.server.state.runtime

import f95gm.server.config.ServerConfig
import f95gm.server.config.ServerConfigSnapshot
import f95gm.server.config.ServerConfigurationState
import f95gm.server.http.request.RequestMetrics
import f95gm.server.kernel.ServerClock
import f95gm.server.persistence.ServerRepositories
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.cache.CacheStatisticsSnapshot
import f95gm.server.observability.StructuredConfigurationReloadEventSink
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import f95gm.server.kernel.AwaitableLifecycleResource
import f95gm.server.kernel.TerminalCloseCoordinator
import kotlin.time.Duration.Companion.milliseconds

internal sealed interface RepositoryDependency {
    val ownership: DependencyOwnership
    data object Default : RepositoryDependency { override val ownership = DependencyOwnership.OWNED }
    data class Provided(val value: ServerRepositories) : RepositoryDependency {
        override val ownership = DependencyOwnership.BORROWED
    }
}

internal sealed interface HttpClientDependency {
    val ownership: DependencyOwnership
    data object Default : HttpClientDependency { override val ownership = DependencyOwnership.OWNED }
    data class Provided(val value: HttpClient) : HttpClientDependency {
        override val ownership = DependencyOwnership.BORROWED
    }
}

internal enum class DependencyOwnership { OWNED, BORROWED }

internal data class ServerRuntimeDependencies(
    val database: TransactionBoundary,
    val repositories: RepositoryDependency = RepositoryDependency.Default,
    val httpClient: HttpClientDependency = HttpClientDependency.Default,
    val dispatcher: CoroutineDispatcher
)

/** Read-only diagnostics separated from runtime lifecycle ownership. */
internal class ServerRuntimeObservabilityFacade(private val assembly: ServerRuntimeAssembly) {
    val requestMetrics: RequestMetrics get() = assembly.observability.requestMetrics
    val snapshot: ServerObservabilitySnapshot
        get() = assembly.observability.snapshot(
            assembly.registrySnapshots.snapshot(assembly.sessions, assembly.idempotency)
        )
    val cacheStatistics: CacheStatisticsSnapshot get() = assembly.cacheStatsProvider.snapshot()
}

/** Lifecycle/composition facade over the construction-only runtime assembly. */
internal class ServerRuntimeServices private constructor(
    private val config: ServerConfig,
    private val clock: ServerClock,
    dependencies: ServerRuntimeDependencies,
    private val constructionRollback: StartupRollbackStack
) : AwaitableLifecycleResource {
    companion object {
        fun create(
            config: ServerConfig,
            clock: ServerClock,
            dependencies: ServerRuntimeDependencies
        ): ServerRuntimeServices = StartupRollbackStack().construct { rollback ->
            ServerRuntimeServices(config, clock, dependencies, rollback)
        }
    }

    private val closeCoordinator = TerminalCloseCoordinator()
    private val configurationController = ServerConfigurationState(
        ServerConfigSnapshot.from(config),
        eventSink = StructuredConfigurationReloadEventSink,
        publicationDispatcher = dependencies.dispatcher
    ).also { constructionRollback.register("configuration-state", it) }
    private val configurationState get() = configurationController
    private val assembly = ServerRuntimeAssembly(
        config, clock, dependencies, { closeCoordinator.isClosed }, constructionRollback
    )
    private val observabilityFacade = ServerRuntimeObservabilityFacade(assembly)

    private val requestMetrics get() = observabilityFacade.requestMetrics
    private val sessions get() = assembly.sessions
    private val activeRequests get() = assembly.activeRequests
    private val persistenceServices get() = assembly.persistenceServices
    private val upstreamServices get() = assembly.upstreamServices

    val applicationComposition: ServerApplicationComposition = ServerApplicationComposition(
            RuntimeComposition(
                clock,
                requestMetrics,
                activeRequests,
                LifecycleScopeHandle.from(assembly.lifecycleScope),
                configurationState,
                assembly.cacheStatsProvider
            ),
            assembly.infrastructureServices,
            PersistenceComposition(persistenceServices.sessionService),
            assembly.routeComposition
        )

    private val lifecycle = ServerRuntimeLifecycle(
        assembly.resources,
        upstreamServices,
        assembly.routeComposition.services,
        sessions,
        clock,
        configurationState,
        config.timeouts.shutdownMillis.value.milliseconds
    )

    override val isClosed: Boolean get() = closeCoordinator.isClosed
    @Suppress("TooGenericExceptionCaught") // The runtime facade publishes the same ordinary or fatal close failure to all awaiters.
    override fun close() {
        if (!closeCoordinator.beginClose()) return
        try {
            lifecycle.close()
            closeCoordinator.complete()
        } catch (failure: Throwable) {
            closeCoordinator.fail(failure)
        }
    }
    @Suppress("TooGenericExceptionCaught") // The runtime facade publishes the same ordinary or fatal close failure to all awaiters.
    override suspend fun closeAndJoin() {
        if (!closeCoordinator.beginClose()) {
            closeCoordinator.await()
            return
        }
        try {
            lifecycle.closeAndJoin()
            closeCoordinator.complete()
        } catch (failure: Throwable) {
            closeCoordinator.fail(failure)
        }
    }
}
