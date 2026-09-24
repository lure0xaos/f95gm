package f95gm.server.state.runtime

import f95gm.server.persistence.sessions.persistSessionFor
import f95gm.server.state.runtime.session.SessionLockRegistry
import f95gm.server.upstream.request.UpstreamCircuitBreaker
import f95gm.server.upstream.request.UpstreamClient
import f95gm.server.upstream.request.UpstreamGateway
import kotlin.time.Duration.Companion.milliseconds

internal class ServerUpstreamServices(
    private val dependencies: ServerUpstreamDependencies
) : LifecycleResource {
    private val lock = Any()
    private val sessionLocks = SessionLockRegistry()
    private val closeGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed
    private var clientInstance: UpstreamClient? = null

    val gateway: UpstreamGateway
        get() = synchronized(lock) {
            check(!closeGuard.isClosed) { "Upstream services are closed" }
            clientInstance ?: UpstreamClient(
                dependencies.client,
                scope = dependencies.scope,
                sessionLocks = sessionLocks,
                circuitBreaker = UpstreamCircuitBreaker(
                    clock = dependencies.clock,
                    openDuration = dependencies.config.traffic.circuitBreakerOpenMillis.value.milliseconds,
                    maxEntries = dependencies.config.traffic.maxCircuitBreakerEntries,
                    onOpened = dependencies.metrics::recordCircuitOpening
                ),
                clock = dependencies.clock,
                policy = dependencies.policy,
                metrics = dependencies.metrics,
                persistSession = { session ->
                    persistSessionFor(
                        dependencies.database,
                        dependencies.sessions,
                        dependencies.clock,
                        session
                    )
                }
            ).also { clientInstance = it }
        }

    override fun close() {
        synchronized(lock) {
            closeGuard.closeOnce {
                clientInstance?.close()
                clientInstance = null
                sessionLocks.close()
            }
        }
    }
}
