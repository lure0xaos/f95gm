package f95gm.server.state.runtime

import f95gm.server.kernel.ServerInvariantViolation

import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource

import f95gm.server.kernel.ResourceLock

import f95gm.server.persistence.sessions.persistSessionFor
import f95gm.server.state.runtime.session.SessionLockRegistry
import f95gm.server.upstream.request.UpstreamCircuitBreaker
import f95gm.server.upstream.request.UpstreamClient
import f95gm.server.upstream.request.UpstreamGateway

internal class ServerUpstreamServices(
    private val dependencies: ServerUpstreamDependencies
) : LifecycleResource {
    private val lock = ResourceLock()
    private val sessionLocks = SessionLockRegistry()
    private val closeGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed
    private var clientInstance: UpstreamClient? = null

    val gateway: UpstreamGateway
        get() = lock.withLock {
            if (closeGuard.isClosed) throw ServerInvariantViolation("Upstream services are closed")
            clientInstance ?: UpstreamClient(
                dependencies.client,
                scope = dependencies.scope,
                sessionLocks = sessionLocks,
                circuitBreaker = UpstreamCircuitBreaker(
                    clock = dependencies.clock,
                    openDuration = dependencies.policy.circuitBreakerOpen,
                    maxEntries = dependencies.policy.maxCircuitBreakerEntries,
                    onOpened = dependencies.metrics::recordCircuitOpening
                ),
                clock = dependencies.clock,
                endpointConfig = dependencies.config.upstreamEndpoints,
                policy = dependencies.policy.request,
                maxConcurrentRequests = dependencies.policy.maxConcurrentRequests,
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
        lock.withLock {
            closeGuard.closeOnce {
                clientInstance?.close()
                clientInstance = null
                sessionLocks.close()
            }
        }
    }
}
