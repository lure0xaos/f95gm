package f95gm.server.desktop

import f95gm.server.http.request.BrowserTabId
import f95gm.server.kernel.BoundedAccessMap
import f95gm.server.kernel.EntryCapacity
import f95gm.server.kernel.MonotonicMillis

/** Tracks browser tabs participating in the desktop shutdown handshake. */
internal class BrowserTabPresenceRegistry(
    private val timeoutMillis: Long,
    maxEntries: Int
) {
    private val tabs = BoundedAccessMap<BrowserTabId, MonotonicMillis>(EntryCapacity(maxEntries))

    fun remove(tabId: BrowserTabId?) {
        tabId?.let(tabs::remove)
    }

    fun heartbeat(tabId: BrowserTabId?, now: MonotonicMillis) {
        tabId?.let { tabs.put(it, now) }
    }

    val hasActiveTabs: Boolean get() = tabs.isNotEmpty()

    fun removeExpired(now: MonotonicMillis) {
        val expiration = now.value - timeoutMillis
        tabs.removeIf { _, lastSeen -> lastSeen.value < expiration }
    }
}
