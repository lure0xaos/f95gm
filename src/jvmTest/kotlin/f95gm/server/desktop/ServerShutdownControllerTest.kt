package f95gm.server.desktop

import f95gm.server.state.runtime.ActiveRequestTracker
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerShutdownControllerTest {
    @Test
    fun requestEntersDrainingStateBeforeStopRuns() {
        val stopped = CountDownLatch(1)
        val controller = ServerShutdownController(stopped::countDown)
        controller.request()
        assertTrue(controller.isDraining)
        assertTrue(stopped.await(1, TimeUnit.SECONDS))
        controller.close()
    }

    @Test
    fun repeatedShutdownRequestsStopOnlyOnce() {
        val stops = AtomicInteger()
        val stopped = CountDownLatch(1)
        val controller = ServerShutdownController({ stops.incrementAndGet(); stopped.countDown() })
        controller.request()
        controller.request()

        assertTrue(stopped.await(1, TimeUnit.SECONDS))
        assertTrue(controller.isDraining)
        assertEquals(1, stops.get())
        controller.close()
    }

    @Test
    fun concurrentShutdownRequestsStillStopOnlyOnce() {
        val stops = AtomicInteger()
        val controller = ServerShutdownController { stops.incrementAndGet() }
        val executor = Executors.newFixedThreadPool(8)
        repeat(32) { executor.submit { controller.request() } }
        executor.shutdown()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
        controller.close()
        assertEquals(1, stops.get())
    }

    @Test
    fun drainingWaitsForActiveRequestsBeforeStopping() = runBlocking {
        val tracker = ActiveRequestTracker()
        val stopped = CountDownLatch(1)
        val controller = ServerShutdownController(stopped::countDown, activeRequests = tracker)
        val lease = tracker.enter()
        try {
            controller.request()
            assertTrue(controller.isDraining)
            assertFalse(stopped.await(100, TimeUnit.MILLISECONDS))
            lease.close()
            assertTrue(stopped.await(1, TimeUnit.SECONDS))
        } finally {
            controller.close()
        }
        assertFalse(controller.isShutdownScopeActive)
    }
}
