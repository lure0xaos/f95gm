package f95gm.server.app

import f95gm.server.desktop.DesktopLifecycle
import f95gm.server.desktop.NoopDesktopLifecycle
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.state.runtime.ServerApplicationComposition
import f95gm.server.kernel.ServerInvariantViolation
import f95gm.server.state.runtime.StartupMetrics
import io.ktor.server.engine.ApplicationEngine
import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.LifecycleResource

internal class ServerRuntimeState(private val lifecycleMetrics: StartupMetrics) : LifecycleResource {
    private val lifecycleLock = ResourceLock()
    private var lifecycle = RuntimeLifecycleState.NEW
    private val server = LifecycleSlot<ApplicationEngine>()
    private val desktop = LifecycleSlot(NoopDesktopLifecycle as DesktopLifecycle)
    private val shutdownController = LifecycleSlot<ServerShutdownController>()

    /** Exposes the lifecycle phase without exposing mutable slots or transition controls. */
    internal val phase: ServerRuntimePhase
        get() = lifecycleLock.withLock { lifecycle.toPublicPhase() }

    override val isClosed: Boolean
        get() = lifecycleLock.withLock { lifecycle == RuntimeLifecycleState.CLOSED }

    fun createShutdownController(composition: ServerApplicationComposition): ServerShutdownController =
        ServerShutdownController({
            server.peek()?.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000)
                ?: throw ServerInvariantViolation("Server engine is not initialized")
        }, composition.runtime.clock, composition.runtime.activeRequests,
            shutdownScope = composition.runtime.lifecycleScope).also {
            shutdownController.bind(it)
        }

    fun bind(server: ApplicationEngine) {
        transition(RuntimeLifecycleState.NEW, RuntimeLifecycleState.SERVER_BOUND, "Server engine can only be bound during startup")
        this.server.bind(server)
    }

    fun bind(desktop: DesktopLifecycle) {
        transition(RuntimeLifecycleState.SERVER_BOUND, RuntimeLifecycleState.RUNNING, "Desktop lifecycle can only be bound after the server engine")
        this.desktop.replace(desktop)
    }

    fun desktopController(): DesktopLifecycle = desktop.get()

    override fun close() {
        val shouldClose = lifecycleLock.withLock {
            if (lifecycle == RuntimeLifecycleState.CLOSED) false else { lifecycle = RuntimeLifecycleState.CLOSED; true }
        }
        if (!shouldClose) return
        RuntimeShutdownPlan.close(
            desktop.take(),
            shutdownController.take(),
            server.take()
        )
    }

    private fun transition(expected: RuntimeLifecycleState, next: RuntimeLifecycleState, message: String) {
        lifecycleLock.withLock {
            check(lifecycle == expected) {
                lifecycleMetrics.lifecycleRejections.incrementAndGet()
                message
            }
            lifecycle = next
        }
    }
}
