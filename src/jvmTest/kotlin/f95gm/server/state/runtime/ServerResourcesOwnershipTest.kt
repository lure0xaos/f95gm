package f95gm.server.state.runtime

import f95gm.server.config.Millis
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerResourcesOwnershipTest {
    @Test
    fun ownedJobsAreCancelledWhenResourcesClose() = runBlocking {
        val resources = ServerResources(
            HttpClientTimeoutPolicy(Millis(1_000), Millis(1_000), Millis(1_000)),
            Dispatchers.Default
        )
        val job = resources.launchOwned { delay(Long.MAX_VALUE) }

        assertTrue(job.isActive)
        resources.close()
        job.join()

        assertFalse(job.isActive)
    }
}
