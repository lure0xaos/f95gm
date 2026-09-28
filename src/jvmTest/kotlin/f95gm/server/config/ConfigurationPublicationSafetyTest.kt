package f95gm.server.config

import f95gm.server.kernel.ShutdownFailure
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ConfigurationPublicationSafetyTest {
    @Test
    fun rejectedReloadPreservesSnapshotAndObserversSeeTheCommittedSnapshot() {
        val initial = ServerConfigSnapshot.from(ServerConfig.from(emptyMap()))
        val state = ServerConfigurationState(initial, maxPendingPublications = 1)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        state.subscribe { result ->
            assertSame(result.current, state.current())
            entered.countDown()
            assertTrue(release.await(5, TimeUnit.SECONDS))
        }
        try {
            val accepted = executor.submit<ConfigurationReloadResult> { state.reloadWithResult(source(false)) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val committed = state.current()
            assertFailsWith<ConfigurationPublicationRejected> { state.reload(source(true)) }
            assertSame(committed, state.current())
            assertEquals(1, state.rejectedPublicationCount)
            release.countDown()
            assertSame(committed, accepted.get(5, TimeUnit.SECONDS).current)
            assertEquals(true, state.reload(source(true)).value.openBrowser)
        } finally {
            release.countDown()
            state.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun nestedReloadAndCloseAreRejectedBeforeMutation() {
        val state = ServerConfigurationState(ServerConfigSnapshot.from(ServerConfig.from(emptyMap())))
        state.subscribe {
            val committed = state.current()
            assertFailsWith<IllegalStateException> { state.reload(source(true)) }
            assertFailsWith<IllegalStateException> { state.close() }
            assertSame(committed, state.current())
        }
        try {
            assertEquals(0, state.reloadWithResult(source(false)).listenerFailures)
            assertEquals(false, state.current().value.openBrowser)
        } finally {
            state.close()
        }
    }

    @Test
    fun synchronousCloseTimesOutAndReleasesActiveAndQueuedWaiters() {
        val dispatcher = ConfigurationPublicationDispatcher(Dispatchers.Default, shutdownTimeout = 50.milliseconds)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val active = dispatcher.enqueue {
                entered.countDown()
                assertTrue(release.await(5, TimeUnit.SECONDS))
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            var queuedRan = false
            val queued = dispatcher.enqueue { queuedRan = true }
            val closing = executor.submit<ShutdownFailure> {
                assertFailsWith<ShutdownFailure> { dispatcher.close() }
            }
            val failure = closing.get(5, TimeUnit.SECONDS)
            assertSame(failure, assertFailsWith<ShutdownFailure> { dispatcher.await(active) })
            assertSame(failure, assertFailsWith<ShutdownFailure> { dispatcher.await(queued) })
            assertEquals(false, queuedRan)
            assertEquals(0, dispatcher.pendingCount)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun suspendingCloseTimesOutAndReleasesQueuedWaiters(): Unit = runBlocking {
        val dispatcher = ConfigurationPublicationDispatcher(Dispatchers.Default, shutdownTimeout = 50.milliseconds)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        try {
            dispatcher.enqueue {
                entered.countDown()
                assertTrue(release.await(5, TimeUnit.SECONDS))
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val queued = dispatcher.enqueue { Unit }
            val failure = assertFailsWith<ShutdownFailure> { dispatcher.closeAndJoin() }
            assertSame(failure, assertFailsWith<ShutdownFailure> { dispatcher.await(queued) })
        } finally {
            release.countDown()
        }
    }

    @Test
    fun workerDetectionDoesNotDependOnItsInitialThread() {
        val dispatcher = ConfigurationPublicationDispatcher(Dispatchers.Default)
        try {
            repeat(20) {
                val task = dispatcher.enqueue {
                    assertFailsWith<IllegalStateException> { dispatcher.dispatch { Unit } }
                }
                dispatcher.await(task)
            }
        } finally {
            dispatcher.close()
        }
    }

    private fun source(openBrowser: Boolean) = object : ConfigurationSource {
        override fun properties() = mapOf("f95gm.openBrowser" to openBrowser.toString())
        override fun environment(): Map<String, String> = emptyMap()
    }
}
