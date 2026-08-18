package f95gm.server.desktop

import f95gm.server.config.logging.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class ServerShutdownController(private val stop: () -> Unit) {
    private companion object {
        const val BROWSER_TAB_TIMEOUT_MILLIS = 10_000L
        const val BROWSER_SHUTDOWN_DELAY_SECONDS = 5L
    }

    private val requested = AtomicBoolean(false)
    private val lock = Any()
    private val browserTabs = ConcurrentHashMap<String, Long>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "f95gm-browser-shutdown").apply { isDaemon = true }
    }
    private var browserShutdown: ScheduledFuture<*>? = null

    fun requestFromBrowser(tabId: String?) {
        synchronized(lock) {
            tabId?.let(browserTabs::remove)
            removeExpiredBrowserTabs()
            if (requested.get() || browserTabs.isNotEmpty()) return
            if (browserShutdown != null) return
            browserShutdown = scheduler.schedule(
                { request() },
                BROWSER_SHUTDOWN_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
        }
    }

    fun browserHeartbeat(tabId: String?) {
        synchronized(lock) {
            removeExpiredBrowserTabs()
            tabId?.takeIf { it.isNotBlank() }?.let { browserTabs[it] = System.currentTimeMillis() }
            cancelBrowserShutdown()
        }
    }

    fun request() {
        synchronized(lock) { cancelBrowserShutdown() }
        if (!requested.compareAndSet(false, true)) return
        scheduler.shutdownNow()
        Thread {
            runCatching { stop() }
                .onFailure { error -> logger.warn(error) { "Could not stop the server gracefully" } }
        }.apply {
            name = "f95gm-shutdown"
            isDaemon = true
        }.start()
    }

    private fun cancelBrowserShutdown() {
        browserShutdown?.cancel(false)
        browserShutdown = null
    }

    private fun removeExpiredBrowserTabs() {
        val expiration = System.currentTimeMillis() - BROWSER_TAB_TIMEOUT_MILLIS
        browserTabs.entries.removeIf { (_, lastSeen) -> lastSeen < expiration }
    }
}
