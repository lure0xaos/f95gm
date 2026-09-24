package f95gm.server.state.runtime

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActiveRequestTrackerTest {
    @Test
    fun waitsForAnActiveRequestToLeave() = runTest {
        val tracker = ActiveRequestTracker()
        val lease = tracker.enter()
        assertFalse(tracker.awaitZero(1))
        lease.close()
        assertTrue(tracker.awaitZero(100))
    }
}
