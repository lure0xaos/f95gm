package f95gm.server.desktop

import f95gm.server.config.logging.logger
import f95gm.server.http.request.BrowserTabId
import f95gm.server.state.runtime.ActiveRequestTracker
import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.SystemServerClock
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class ServerShutdownController(
    private val stop: () -> Unit,
    private val clock: ServerClock = SystemServerClock,
    private val activeRequests: ActiveRequestTracker? = null,
    private val drainTimeout: Duration = 5.seconds
) {
    internal constructor(stop: () -> Unit) : this(stop, SystemServerClock)
    private companion object {
        const val BROWSER_TAB_TIMEOUT_MILLIS = 10_000L
        const val BROWSER_SHUTDOWN_DELAY_SECONDS = 5L
        const val MAX_BROWSER_TABS = 512
    }

    private enum class ShutdownState { RUNNING, DRAINING, STOPPED }

    @Volatile
    private var state = ShutdownState.RUNNING
    private val lock = Any()
    private val browserTabs = LinkedHashMap<String, Long>(MAX_BROWSER_TABS, 0.75f, true)
    private val scheduler = BrowserShutdownScheduler()
    private var browserShutdown: ScheduledFuture<*>? = null
    private val shutdownScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var shutdownJob: Job? = null
    private val shutdownCompleted = CountDownLatch(1)

    fun requestFromBrowser(tabId: String?) {
        requestFromBrowser(tabId?.takeIf { it.isNotBlank() }?.let { runCatching { BrowserTabId(it) }.getOrNull() })
    }

    fun requestFromBrowser(tabId: BrowserTabId?) {
        synchronized(lock) {
            tabId?.let { browserTabs.remove(it.value) }
            removeExpiredBrowserTabs()
            if (state != ShutdownState.RUNNING || browserTabs.isNotEmpty()) return
            if (browserShutdown != null) return
            browserShutdown = scheduler.schedule(BROWSER_SHUTDOWN_DELAY_SECONDS, ::request)
        }
    }

    fun browserHeartbeat(tabId: String?) {
        browserHeartbeat(tabId?.takeIf { it.isNotBlank() }?.let { runCatching { BrowserTabId(it) }.getOrNull() })
    }

    fun browserHeartbeat(tabId: BrowserTabId?) {
        synchronized(lock) {
            removeExpiredBrowserTabs()
            tabId?.let {
                browserTabs[it.value] = clock.nowMillis()
                while (browserTabs.size > MAX_BROWSER_TABS) browserTabs.entries.iterator().apply { next(); remove() }
            }
            cancelBrowserShutdown()
        }
    }

    fun request() {
        synchronized(lock) {
            cancelBrowserShutdown()
            if (state != ShutdownState.RUNNING) return
            state = ShutdownState.DRAINING
        }
        scheduler.close()
        shutdownJob = shutdownScope.launch {
            try {
                activeRequests?.awaitZero(drainTimeout)
                stop()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.warn(error) { "Could not stop the server gracefully" }
            } finally {
                synchronized(lock) {
                    state = ShutdownState.STOPPED
                    shutdownCompleted.countDown()
                }
            }
        }
    }

    fun isDraining(): Boolean = state != ShutdownState.RUNNING

    fun close() {
        synchronized(lock) {
            cancelBrowserShutdown()
            if (state == ShutdownState.RUNNING) {
                state = ShutdownState.STOPPED
                shutdownCompleted.countDown()
            }
        }
        scheduler.close()
        shutdownScope.cancel()
        shutdownCompleted.await(1, TimeUnit.SECONDS)
    }

    private fun cancelBrowserShutdown() {
        scheduler.cancel(browserShutdown)
        browserShutdown = null
    }

    private fun removeExpiredBrowserTabs() {
        val expiration = clock.nowMillis() - BROWSER_TAB_TIMEOUT_MILLIS
        browserTabs.entries.removeIf { (_, lastSeen) -> lastSeen < expiration }
    }
}
