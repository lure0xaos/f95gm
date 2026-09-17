package f95gm.client.state.runtime

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ClientControllerTest {
    @Test
    fun clientStateAcceptsAnInjectedController() {
        val controller = ClientController()
        val state = ClientState(controller)

        assertSame(controller, state.controller)
        assertNotSame(state, ClientState())
        controller.close()
    }

    @Test
    fun loadingStatesAreIndependentAndResetTogether() {
        val controller = ClientController()

        controller.markLoaded(ClientLoadState.SESSION)
        controller.markLoaded(ClientLoadState.MARKS)

        assertTrue(controller.isLoaded(ClientLoadState.SESSION))
        assertTrue(controller.isLoaded(ClientLoadState.MARKS))
        assertFalse(controller.isLoaded(ClientLoadState.SAVED_FILTERS))

        controller.resetSessionLifecycle()

        assertFalse(controller.isLoaded(ClientLoadState.SESSION))
        assertFalse(controller.isLoaded(ClientLoadState.MARKS))
        assertFalse(controller.isLoaded(ClientLoadState.SAVED_FILTERS))
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
        assertTrue(controller.rootStoreJob.isActive)
        val newJob = controller.scope.launch { awaitCancellation() }
        controller.replaceJob(ClientJobKey.JVM_CONNECTION, newJob)
        assertTrue(controller.isJobActive(ClientJobKey.JVM_CONNECTION))
        controller.cancelAllJobs()
        controller.scope.cancel()
        controller.close()
    }

    @Test
    fun closingTheControllerCancelsTheRootStoreLifecycle() {
        val controller = ClientController()

        controller.close()

        assertTrue(controller.rootStoreJob.isCancelled)
        assertFalse(controller.scope.isActive)
        assertFailsWith<IllegalStateException> { controller.replaceScope() }
    }

    @Test
    fun launchOperationRegistersAndCleansUpItsJob() {
        val controller = ClientController()
        val job = controller.launchOperation(ClientJobKey.CATALOG_LOAD) { awaitCancellation() }

        assertSame(job, controller.currentJob(ClientJobKey.CATALOG_LOAD))
        job.cancel()
        assertNull(controller.currentJob(ClientJobKey.CATALOG_LOAD))
        controller.close()
    }
}
