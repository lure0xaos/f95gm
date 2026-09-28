package f95gm.server.state.runtime

import f95gm.server.kernel.ServerInvariantViolation
import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.AwaitableLifecycleResource
import f95gm.server.kernel.OwnedLifecycleResource
import f95gm.server.kernel.LifecycleResourceName
import f95gm.server.kernel.LifecycleResources
import io.ktor.client.HttpClient

import f95gm.server.upstream.request.UpstreamHttpClient
import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.remainingShutdownTimeout
import f95gm.server.kernel.shutdownTimeoutViolation
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

internal data class ResourceLeakReport(val activeOwnedJobs: Int, val activeChildren: Int) {
    val hasLeaks: Boolean get() = activeOwnedJobs != 0 || activeChildren != 0
}

internal class ServerResources(
    timeouts: HttpClientTimeoutPolicy,
    dispatcher: CoroutineDispatcher,
    private val clientDependency: HttpClientDependency = HttpClientDependency.Default,
    private val shutdownTimeout: Duration = 10.seconds
) : AwaitableLifecycleResource, OwnedLifecycleResource {
    internal constructor(
        timeouts: HttpClientTimeoutPolicy,
        dispatcher: CoroutineDispatcher,
        client: HttpClient?,
        shutdownTimeout: Duration = 10.seconds
    ) : this(
        timeouts,
        dispatcher,
        client?.let(HttpClientDependency::Provided) ?: HttpClientDependency.Default,
        shutdownTimeout
    )
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    override val owner: LifecycleResourceName = LifecycleResources.RESOURCES
    private val ownershipLock = ResourceLock()
    private val shutdown = ResourceShutdownCoordinator()
    /** Read-only parent context for composing child services without exposing this scope's lifecycle controls. */
    val coroutineContext: CoroutineContext get() = scope.coroutineContext

    /** Creates a child scope whose lifetime is owned by this resource. */
    fun createChildScope(): OwnedLifecycleScope = OwnedLifecycleScope.childOf(scope)
    private val ownedJobs = ConcurrentHashMap.newKeySet<Job>()

    /** Launches work owned by this resource; all such jobs are cancelled by [close]. */
    fun launchOwned(block: suspend CoroutineScope.() -> Unit): Job {
        ownershipLock.withLock {
            check(!closeGuard.isClosed) { "Cannot launch owned work after resources are closed" }
            val job = scope.launch(block = block)
            ownedJobs += job
            job.invokeOnCompletion { ownershipLock.withLock { ownedJobs -= job } }
            return job
        }
    }

    val activeOwnedJobCount: Int get() = ownedJobs.count { it.isActive }
    internal val shutdownPhase get() = shutdown.phase
    fun leakReport(): ResourceLeakReport = ResourceLeakReport(
        activeOwnedJobs = activeOwnedJobCount,
        activeChildren = scope.coroutineContext[Job]?.children?.count { it.isActive } ?: 0
    )

    /** Deterministic lifecycle hook for shutdown tests and suspend-aware callers. */
    suspend fun awaitOwnedJobs() {
        val jobs = ownershipLock.withLock { ownedJobs.toList() }
        jobs.joinAll()
    }

    /** Cancels owned work, waits for cooperative completion, then closes the owned client. */
    override suspend fun closeAndJoin() {
        if (!shutdown.tryStart()) {
            shutdown.await()
            return
        }
        shutdown.runSuspendTerminal {
            beginShutdown()
            try {
                withTimeout(remainingShutdownTimeout(shutdownTimeout)) { awaitOwnedJobs() }
            } catch (error: TimeoutCancellationException) {
                throw shutdownTimeoutViolation("owned resources", error, activeWork = activeOwnedJobCount)
            }
            val leaks = leakReport()
            if (leaks.hasLeaks) throw ServerInvariantViolation("Resource shutdown leaked coroutine work: $leaks")
            closeClientAndComplete()
        }
    }
    private val closeGuard = CloseGuard()
    private val clientCloseGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed
    private val managedHttpClient = ManagedHttpClient(timeouts, clientDependency)
    val httpClient: io.ktor.client.HttpClient get() = managedHttpClient.client
    val upstreamHttpClient: UpstreamHttpClient get() = managedHttpClient.upstream

    /** Non-blocking AutoCloseable bridge; callers needing completion must use [closeAndJoin]. */
    override fun close() = closeNonBlocking()

    /** Starts cancellation and returns before cooperative child shutdown completes. */
    fun closeNonBlocking() {
        if (shutdown.tryStart()) {
            beginShutdown()
            completeWhenScopeTerminates()
        }
    }

    private fun requestClose() = closeGuard.closeOnce { scope.cancel() }

    private fun beginShutdown() {
        requestClose()
        shutdown.beginWaitingForJobs()
    }

    private fun completeWhenScopeTerminates() {
        val job = scope.coroutineContext[Job]
        if (job == null) {
            closeClientAndComplete()
            return
        }
        job.invokeOnCompletion {
            shutdown.runTerminal {
                closeClientAndComplete()
            }
        }
    }

    private fun closeClientAndComplete() {
        shutdown.beginClientClose()
        closeClient()
        shutdown.complete()
    }

    private fun closeClient() = clientCloseGuard.closeOnce {
        managedHttpClient.close()
    }
}
