package f95gm.server.state.runtime

import f95gm.server.kernel.ServerInvariantViolation
import f95gm.server.kernel.ShutdownFailure
import f95gm.server.kernel.LifecycleResourceName
import f95gm.server.kernel.AwaitableLifecycleResource
import f95gm.server.kernel.LifecycleResources
import f95gm.server.kernel.LifecycleCallbackResource
import f95gm.server.config.ConfigurationReloadListener
import f95gm.server.config.ConfigurationSource
import f95gm.server.config.ServerConfig
import f95gm.server.config.ServerConfigSnapshot
import f95gm.server.config.ServerConfigurationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ServerLifecycleTest {
    @Test
    fun closesResourcesInReverseRegistrationOrderAndOnlyOnce() {
        val lifecycle = ServerLifecycle()
        val closed = mutableListOf<String>()
        lifecycle.register(LifecycleResourceName("first"), LifecycleCallbackResource { closed += "first" })
        lifecycle.register(LifecycleResourceName("second"), LifecycleCallbackResource { closed += "second" })

        lifecycle.close()
        lifecycle.close()

        assertEquals(listOf("second", "first"), closed)
        assertEquals(true, lifecycle.isClosed)
    }

    @Test
    fun registeringAfterCloseClosesTheResourceImmediately() {
        val lifecycle = ServerLifecycle()
        lifecycle.close()
        var closed = false

        lifecycle.register(LifecycleResourceName("resource"), LifecycleCallbackResource { closed = true })
        assertEquals(true, closed)
    }

    @Test
    fun concurrentRegistrationAndCloseNeverClosesAResourceTwice() {
        val lifecycle = ServerLifecycle()
        val closes = AtomicInteger()
        val registered = AtomicInteger()
        val nameSequence = AtomicInteger()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(4)

        repeat(32) {
            executor.submit {
                start.await()
                runCatching {
                    lifecycle.register(
                        LifecycleResourceName("resource-${nameSequence.incrementAndGet()}"),
                        LifecycleCallbackResource { closes.incrementAndGet() }
                    )
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

        assertEquals(true, lifecycle.isClosed)
        assertEquals(registered.get(), closes.get())
    }

    @Test
    fun closeAttemptsEveryResourceAndReportsAllFailures() {
        val lifecycle = ServerLifecycle()
        val closed = AtomicInteger()
        lifecycle.register(
            LifecycleResourceName("first"),
            LifecycleCallbackResource { closed.incrementAndGet(); error("first") }
        )
        lifecycle.register(
            LifecycleResourceName("second"),
            LifecycleCallbackResource { closed.incrementAndGet(); error("second") }
        )

        val failure = assertFailsWith<ServerLifecycleCloseException> { lifecycle.close() }

        assertEquals(2, closed.get())
        assertEquals(1, failure.suppressedExceptions.size)
    }

    @Test
    fun closesInReverseOrderWhenResourcesAreRegisteredAsBundles() {
        val lifecycle = ServerLifecycle()
        val closed = mutableListOf<String>()
        lifecycle.register(LifecycleResourceName("infrastructure"), LifecycleCallbackResource { closed += "infrastructure" })
        lifecycle.register(LifecycleResourceName("routes"), LifecycleCallbackResource { closed += "routes" })
        lifecycle.close()

        assertEquals(listOf("routes", "infrastructure"), closed)
    }

    @Test
    fun rejectsDependenciesThatWereNotRegisteredBeforeTheResource() {
        val lifecycle = ServerLifecycle()
        assertFailsWith<IllegalStateException> {
            lifecycle.register(
                LifecycleResourceName("routes"),
                LifecycleCallbackResource {},
                dependsOn = setOf(LifecycleResourceName("resources"))
            )
        }
    }

    @Test
    fun rejectsDependencyCyclesBeforeLifecycleConstruction() {
        assertFailsWith<IllegalStateException> {
            LifecycleDependencyGraph.validate(
                mapOf(
                    LifecycleResourceName("routes") to setOf(LifecycleResourceName("upstream")),
                    LifecycleResourceName("upstream") to setOf(LifecycleResourceName("routes"))
                )
            )
        }
    }

    @Test
    fun lifecycleValidationReportsDomainInvariantFailures() {
        val failure = assertFailsWith<ServerInvariantViolation> {
            LifecycleDependencyGraph.validate(
                mapOf(LifecycleResourceName("routes") to setOf(LifecycleResourceName("missing")))
            )
        }
        assertEquals(true, failure.message?.contains("unregistered") == true)
    }

    @Test
    fun lifecycleValidationResultContainsActionableGraphDiagnostics() {
        val result = LifecycleDependencyGraph.validateResult(
            mapOf(LifecycleResourceName("routes") to setOf(LifecycleResourceName("missing")))
        )
        assertEquals(true, result is LifecycleDependencyGraph.ValidationResult.Invalid)
        assertEquals(true, (result as LifecycleDependencyGraph.ValidationResult.Invalid).message.contains("unregistered"))
    }

    @Test
    fun closeAndJoinReportsTimeoutFromAStuckAwaitableResource() = runBlocking {
        val lifecycle = ServerLifecycle(kotlin.time.Duration.parse("10ms"))
        lifecycle.register(LifecycleResourceName("stuck"), StuckResource(100))

        val failure = assertFailsWith<ShutdownFailure> { lifecycle.closeAndJoin() }
        assertEquals(true, failure.message?.contains("Timed out") == true)
        assertEquals("server resources", failure.resource.value)
    }

    @Test
    fun closeAndJoinPreservesCancellationFromAResource() = runBlocking {
        val lifecycle = ServerLifecycle(kotlin.time.Duration.parse("1s"))
        lifecycle.register(LifecycleResourceName("cancelling"), CancellingResource())

        assertFailsWith<CancellationException> { lifecycle.closeAndJoin() }
        Unit
    }

    @Test
    fun closeAndJoinIsIdempotentAfterSuccessfulClose() = runBlocking {
        val lifecycle = ServerLifecycle()
        val closes = AtomicInteger()
        lifecycle.register(LifecycleResourceName("awaitable"), object : AwaitableLifecycleResource {
            override val isClosed get() = closes.get() > 0
            override fun close() { closes.incrementAndGet() }
            override suspend fun closeAndJoin() { closes.incrementAndGet() }
        })

        lifecycle.closeAndJoin()
        lifecycle.closeAndJoin()

        assertEquals(1, closes.get())
    }

    @Test
    fun registrationAfterAsyncCloseIsClosedImmediately() = runBlocking {
        val lifecycle = ServerLifecycle()
        lifecycle.closeAndJoin()
        var closed = false

        lifecycle.register(LifecycleResourceName("resource"), LifecycleCallbackResource { closed = true })

        assertEquals(true, closed)
    }

    @Test
    fun configurationPublicationDrainsAfterDependentsClose() {
        val publicationStarted = CountDownLatch(1)
        val releasePublication = CountDownLatch(1)
        val routeClosed = CountDownLatch(1)
        val events = CopyOnWriteArrayList<String>()
        val configuration = ServerConfigurationState(ServerConfigSnapshot.from(ServerConfig.from(emptyMap())))
        configuration.subscribe(ConfigurationReloadListener {
            publicationStarted.countDown()
            releasePublication.await()
        })

        val reloadExecutor = Executors.newSingleThreadExecutor()
        val reload = reloadExecutor.submit {
            configuration.reload(object : ConfigurationSource {
                override fun properties() = mapOf("f95gm.openBrowser" to "false")
                override fun environment(): Map<String, String> = emptyMap()
            })
        }
        assertTrue(publicationStarted.await(2, java.util.concurrent.TimeUnit.SECONDS))

        val lifecycle = ServerLifecycle()
        lifecycle.register(LifecycleResources.CONFIGURATION, configuration)
        lifecycle.register(
            LifecycleResources.ROUTES,
            LifecycleCallbackResource { events += "routes"; routeClosed.countDown() },
            dependsOn = setOf(LifecycleResources.CONFIGURATION)
        )
        val closeExecutor = Executors.newSingleThreadExecutor()
        val close = closeExecutor.submit { lifecycle.close(); events += "closed" }

        assertTrue(routeClosed.await(2, java.util.concurrent.TimeUnit.SECONDS))
        assertEquals(listOf("routes"), events)
        assertFalse(close.isDone)

        releasePublication.countDown()
        close.get(2, java.util.concurrent.TimeUnit.SECONDS)
        reload.get(2, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(listOf("routes", "closed"), events)
        reloadExecutor.shutdownNow()
        closeExecutor.shutdownNow()
    }

    private class StuckResource(private val millis: Long) : AwaitableLifecycleResource {
        override val isClosed: Boolean = false
        override fun close() = Unit
        override suspend fun closeAndJoin() { delay(millis) }
    }

    private class CancellingResource : AwaitableLifecycleResource {
        override val isClosed: Boolean = false
        override fun close() = Unit
        override suspend fun closeAndJoin(): Unit = throw CancellationException("injected shutdown cancellation")
    }
}

