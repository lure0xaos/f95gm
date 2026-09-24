package f95gm.server.state.runtime

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerLifecycleTest {
    @Test
    fun closesResourcesInReverseRegistrationOrderAndOnlyOnce() {
        val lifecycle = ServerLifecycle()
        val closed = mutableListOf<String>()
        lifecycle.register(AutoCloseable { closed += "first" })
        lifecycle.register(AutoCloseable { closed += "second" })

        lifecycle.close()
        lifecycle.close()

        assertEquals(listOf("second", "first"), closed)
        assertEquals(true, lifecycle.isClosed())
    }

    @Test
    fun registeringAfterCloseClosesTheResourceImmediately() {
        val lifecycle = ServerLifecycle()
        lifecycle.close()
        var closed = false

        lifecycle.register(AutoCloseable { closed = true })
        assertEquals(true, closed)
    }

    @Test
    fun concurrentRegistrationAndCloseNeverClosesAResourceTwice() {
        val lifecycle = ServerLifecycle()
        val closes = AtomicInteger()
        val registered = AtomicInteger()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(4)

        repeat(32) {
            executor.submit {
                start.await()
                runCatching {
                    lifecycle.register(AutoCloseable { closes.incrementAndGet() })
                    registered.incrementAndGet()
                }
            }
        }
        executor.submit {
            start.await()
            lifecycle.close()
        }
        start.countDown()
        executor.shutdown()
        check(executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS))
        lifecycle.close()

        assertEquals(true, lifecycle.isClosed())
        assertEquals(registered.get(), closes.get())
    }

    @Test
    fun closeAttemptsEveryResourceAndReportsAllFailures() {
        val lifecycle = ServerLifecycle()
        val closed = AtomicInteger()
        lifecycle.register(AutoCloseable { closed.incrementAndGet(); error("first") })
        lifecycle.register(AutoCloseable { closed.incrementAndGet(); error("second") })

        val failure = assertFailsWith<ServerLifecycleCloseException> { lifecycle.close() }

        assertEquals(2, closed.get())
        assertEquals(1, failure.suppressedExceptions.size)
    }
}
