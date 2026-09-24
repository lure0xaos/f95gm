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

        controller.setSessionLoaded(true)
        controller.setMarksLoaded(true)

        assertTrue(controller.isSessionLoaded())
        assertTrue(controller.areMarksLoaded())
        assertFalse(controller.areSavedFiltersLoaded())

        controller.resetSessionLifecycle()

        assertFalse(controller.isSessionLoaded())
        assertFalse(controller.areMarksLoaded())
        assertFalse(controller.areSavedFiltersLoaded())
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
