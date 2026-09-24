package f95gm.client.state.runtime

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientControllerTest {
    @Test
    fun loadingStatesAreIndependentAndResetTogether() {
        val controller = ClientController()

        controller.sessionLoaded = true
        controller.marksLoaded = true

        assertTrue(controller.sessionLoaded)
        assertTrue(controller.marksLoaded)
        assertFalse(controller.savedFiltersLoaded)

        controller.resetSessionLifecycle()

        assertFalse(controller.sessionLoaded)
        assertFalse(controller.marksLoaded)
        assertFalse(controller.savedFiltersLoaded)
    }

    @Test
    fun replacingTheScopeCancelsOldJobsAndCreatesAUsableRegistry() {
        val controller = ClientController()
        val oldScope = controller.scope
        val oldJob = oldScope.launch { awaitCancellation() }
        controller.replaceJob(ClientJobKey.JVM_CONNECTION, oldJob)

        controller.replaceScope()

        assertTrue(oldJob.isCancelled)
        assertFalse(oldScope.isActive)
        assertTrue(controller.scope.isActive)
        val newJob = controller.scope.launch { awaitCancellation() }
        controller.replaceJob(ClientJobKey.JVM_CONNECTION, newJob)
        assertTrue(controller.isJobActive(ClientJobKey.JVM_CONNECTION))
        controller.cancelAllJobs()
        controller.scope.cancel()
    }
}
