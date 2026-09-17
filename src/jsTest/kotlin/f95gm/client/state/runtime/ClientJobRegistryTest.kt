package f95gm.client.state.runtime

import kotlinx.coroutines.Job
import kotlin.test.*

class ClientJobRegistryTest {
    @Test
    fun replacingAJobCancelsThePreviousAndTracksTheNewOne() {
        val registry = ClientJobRegistry()
        val first = Job()
        val second = Job()

        registry.replace(ClientJobKey.CATALOG_LOAD, first)
        registry.replace(ClientJobKey.CATALOG_LOAD, second)

        assertTrue(first.isCancelled)
        assertSame(second, registry.current(ClientJobKey.CATALOG_LOAD))
        assertTrue(registry.isActive(ClientJobKey.CATALOG_LOAD))

        second.cancel()
        assertNull(registry.current(ClientJobKey.CATALOG_LOAD))
        assertFalse(registry.isActive(ClientJobKey.CATALOG_LOAD))
    }

    @Test
    fun completionOfAnOldJobCannotRemoveItsReplacement() {
        val registry = ClientJobRegistry()
        val first = Job()
        val second = Job()

        registry.replace(ClientJobKey.VERSION_CHECK, first)
        registry.replace(ClientJobKey.VERSION_CHECK, second)
        registry.removeIfCurrent(ClientJobKey.VERSION_CHECK, first)

        assertSame(second, registry.current(ClientJobKey.VERSION_CHECK))
    }

    @Test
    fun cancelAllAndClosePreventFurtherWork() {
        val registry = ClientJobRegistry()
        val first = Job()
        val second = Job()
        registry.replace(ClientJobKey.CATALOG_LOAD, first)
        registry.replace(ClientJobKey.JVM_CONNECTION, second)

        registry.close()
        registry.close()

        assertTrue(first.isCancelled)
        assertTrue(second.isCancelled)
        assertEquals(null, registry.current(ClientJobKey.CATALOG_LOAD))
        assertEquals(null, registry.current(ClientJobKey.JVM_CONNECTION))
        assertFailsWith<IllegalStateException> {
            registry.replace(ClientJobKey.CATALOG_LOAD, Job())
        }
    }
}
