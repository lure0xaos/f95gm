package f95gm.server.desktop

import f95gm.server.state.runtime.ActiveRequestTracker
import f95gm.server.state.runtime.MutableServerClock
import f95gm.server.state.runtime.LifecycleScopeHandle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ServerShutdownControllerTest {
    @Test
    fun browserTabHeartbeatCancelsPendingShutdownUntilTabIsRemoved() {
        val stops = AtomicInteger()
        val controller = ServerShutdownController(
            { stops.incrementAndGet() },
            clock = MutableServerClock(),
            shutdownScope = LifecycleScopeHandle.test()
        )
        try {
            controller.requestFromBrowser(null as String?)
            controller.browserHeartbeat("tab-1")
            controller.requestFromBrowser(null as String?)

            assertFalse(controller.isDraining)
            assertEquals(0, stops.get())

            controller.requestFromBrowser("tab-1")
            assertFalse(controller.isDraining)
            assertEquals(0, stops.get())
        } finally {
            controller.close()
        }
    }

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

    @Test
    fun closeAndAwaitWaitsForAnActiveShutdownBeforeClosingItsScope() = runBlocking {
        val tracker = ActiveRequestTracker()
        val stopped = CountDownLatch(1)
        val controller = ServerShutdownController(stopped::countDown, activeRequests = tracker)
        val lease = tracker.enter()
        try {
            controller.request()
            val closing = async { controller.closeAndAwait(2.seconds) }

            assertFalse(stopped.await(100, TimeUnit.MILLISECONDS))
            lease.close()

            assertTrue(closing.await())
            assertTrue(stopped.await(1, TimeUnit.SECONDS))
            assertFalse(controller.isShutdownScopeActive)
        } finally {
            lease.close()
            controller.close()
        }
    }

    @Test
    fun closeAndAwaitWaitsForBlockedStopOperationBeforeClosingItsScope() = runBlocking {
        val stopStarted = CountDownLatch(1)
        val releaseStop = CountDownLatch(1)
        val controller = ServerShutdownController {
            stopStarted.countDown()
            check(releaseStop.await(2, TimeUnit.SECONDS))
        }
        try {
            controller.request()
            assertTrue(stopStarted.await(1, TimeUnit.SECONDS))
            val closing = async { controller.closeAndAwait(2.seconds) }

            assertFalse(closing.isCompleted)
            releaseStop.countDown()
            assertTrue(closing.await())
        } finally {
            releaseStop.countDown()
            controller.close()
        }
    }
}
