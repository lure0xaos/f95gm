package f95gm.server.desktop

import f95gm.server.http.request.BrowserTabId
import f95gm.server.state.runtime.ActiveRequestTracker
import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.ResourceLock
import f95gm.server.platform.SystemServerClock
import f95gm.server.state.runtime.LifecycleScopeHandle
import f95gm.server.kernel.ShutdownCoordinator
import f95gm.server.kernel.remainingShutdownTimeout
import f95gm.server.kernel.ValidatedState
import f95gm.server.state.runtime.runTerminalBoundary
import kotlinx.coroutines.*
import java.util.concurrent.ScheduledFuture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class ServerShutdownController(
    private val stop: () -> Unit,
    private val clock: ServerClock = SystemServerClock,
    private val activeRequests: ActiveRequestTracker? = null,
    private val drainTimeout: Duration = 5.seconds,
    private val shutdownScope: LifecycleScopeHandle
) {
    /** Test-only convenience constructor; production supplies the server lifecycle scope explicitly. */
    internal constructor(stop: () -> Unit) : this(stop, shutdownScope = LifecycleScopeHandle.test())

    /** Test-only convenience constructor retaining the active-request drain fixture. */
    internal constructor(stop: () -> Unit, activeRequests: ActiveRequestTracker?) : this(
        stop,
        activeRequests = activeRequests,
        shutdownScope = LifecycleScopeHandle.test()
    )
    private companion object {
        const val BROWSER_TAB_TIMEOUT_MILLIS = 10_000L
        const val BROWSER_SHUTDOWN_DELAY_SECONDS = 5
        const val MAX_BROWSER_TABS = 512
    }

    private val state = ValidatedState(
        ServerShutdownState.RUNNING,
        mapOf(
            ServerShutdownState.RUNNING to setOf(ServerShutdownState.DRAINING, ServerShutdownState.STOPPED),
            ServerShutdownState.DRAINING to setOf(ServerShutdownState.STOPPED),
            ServerShutdownState.STOPPED to emptySet()
        )
    )
    private val lock = ResourceLock()
    private val browserTabs = BrowserTabPresenceRegistry(BROWSER_TAB_TIMEOUT_MILLIS, MAX_BROWSER_TABS)
    private val scheduler = BrowserShutdownScheduler()
    private var browserShutdown: ScheduledFuture<*>? = null
    private var shutdownJob: Job? = null
    private val shutdownCompletion = ShutdownCoordinator()

    fun requestFromBrowser(tabId: String?) {
        requestFromBrowser(tabId?.takeIf { it.isNotBlank() }?.let(::parseBrowserTabId))
    }

    fun requestFromBrowser(tabId: BrowserTabId?) {
        lock.withLock {
            browserTabs.remove(tabId)
            removeExpiredBrowserTabs()
            if (state.current() != ServerShutdownState.RUNNING || browserTabs.hasActiveTabs()) return
            if (browserShutdown != null) return
            browserShutdown = scheduler.schedule(BROWSER_SHUTDOWN_DELAY_SECONDS.seconds, ::request)
        }
    }

    fun browserHeartbeat(tabId: String?) {
        browserHeartbeat(tabId?.takeIf { it.isNotBlank() }?.let(::parseBrowserTabId))
    }

    fun browserHeartbeat(tabId: BrowserTabId?) {
        lock.withLock {
            removeExpiredBrowserTabs()
            browserTabs.heartbeat(tabId, clock.monotonicMillisTimestamp())
            cancelBrowserShutdown()
        }
    }

    @Suppress("TooGenericExceptionCaught", "ThrowsCount") // Publish both ordinary and fatal terminal shutdown failures.
    fun request() {
        lock.withLock {
            cancelBrowserShutdown()
            if (state.current() != ServerShutdownState.RUNNING) return
            state.transition(ServerShutdownState.DRAINING)
        }
        scheduler.close()
        val job = shutdownScope.launch(start = CoroutineStart.LAZY) {
            runTerminalBoundary(shutdownCompletion) {
                try {
                    ServerShutdownDrain(activeRequests, drainTimeout, stop).execute()
                    shutdownCompletion.complete()
                } finally {
                    lock.withLock { state.transition(ServerShutdownState.STOPPED, allowRepeated = true) }
                }
            }
        }
        lock.withLock { shutdownJob = job }
        job.invokeOnCompletion { cause ->
            if (cause != null) shutdownCompletion.fail(cause)
        }
        job.start()
    }

    val shutdownState: ServerShutdownState get() = state.current()
    val isDraining: Boolean get() = shutdownState != ServerShutdownState.RUNNING
    internal val isShutdownScopeActive: Boolean get() = shutdownScope.isActive

    internal suspend fun awaitShutdown(timeout: Duration = 1.seconds): Boolean =
        withTimeoutOrNull(remainingShutdownTimeout(timeout)) {
            val job = lock.withLock { shutdownJob }
            if (job == null) true else shutdownCompletion.await().let { job.join(); true }
        } ?: false

    fun close() {
        lock.withLock {
            cancelBrowserShutdown()
            if (state.current() == ServerShutdownState.RUNNING) {
                state.transition(ServerShutdownState.STOPPED)
            }
        }
        scheduler.close()
        if (lock.withLock { shutdownJob == null }) shutdownCompletion.complete()
        shutdownScope.cancel()
    }

    /** Suspend-aware process boundary for callers that must observe shutdown completion. */
    internal suspend fun closeAndAwait(timeout: Duration = 1.seconds): Boolean {
        close()
        return awaitShutdown(timeout)
    }

    private fun cancelBrowserShutdown() {
        scheduler.cancel(browserShutdown)
        browserShutdown = null
    }

    private fun removeExpiredBrowserTabs() {
        browserTabs.removeExpired(clock.monotonicMillisTimestamp())
    }

    private fun parseBrowserTabId(value: String): BrowserTabId? = BrowserTabId.parseOrNull(value)
}
