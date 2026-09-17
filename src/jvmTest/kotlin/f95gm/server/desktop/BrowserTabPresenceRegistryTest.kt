package f95gm.server.desktop

import f95gm.server.http.request.BrowserTabId
import f95gm.server.kernel.MonotonicMillis
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowserTabPresenceRegistryTest {
    @Test
    fun activeTabPropertyTracksHeartbeatsRemovalAndExpiration() {
        val registry = BrowserTabPresenceRegistry(timeoutMillis = 100L, maxEntries = 4)
        val tab = BrowserTabId("tab-1")

        assertFalse(registry.hasActiveTabs)

        registry.heartbeat(tab, MonotonicMillis(10L))
        assertTrue(registry.hasActiveTabs)

        registry.remove(tab)
        assertFalse(registry.hasActiveTabs)

        registry.heartbeat(tab, MonotonicMillis(10L))
        registry.removeExpired(MonotonicMillis(111L))
        assertFalse(registry.hasActiveTabs)
    }
}
