package f95gm.server.state.runtime

import f95gm.server.config.Millis
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerResourcesTest {
    @Test
    fun closeCancelsScopeWithoutClosingAnInjectedClient() = runBlocking {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val resources =
            ServerResources(HttpClientTimeoutPolicy(Millis(1_000), Millis(1_000), Millis(1_000)), Dispatchers.Default, client = client)
        val started = CompletableDeferred<Unit>()
        val job = resources.launchOwned {
            started.complete(Unit)
            awaitCancellation()
        }
        started.await()
        assertEquals(1, resources.activeOwnedJobCount)

        resources.close()
        resources.close()
        job.join()

        assertTrue(resources.isClosed)
        assertTrue(job.isCancelled)
        assertEquals(0, resources.activeOwnedJobCount)
        assertEquals("ok", client.get("https://fixture.test/").bodyAsText())
        client.close()
    }

    @Test
    fun ownedWorkCancellationDoesNotPreventDeterministicShutdown() = runBlocking {
        val resources = ServerResources(HttpClientTimeoutPolicy(Millis(1_000), Millis(1_000), Millis(1_000)), Dispatchers.Default)
        val cancelled = resources.launchOwned { kotlinx.coroutines.awaitCancellation() }

        resources.close()

        assertTrue(resources.isClosed)
        assertTrue(cancelled.isCancelled)
        assertEquals(0, resources.activeOwnedJobCount)
    }

    @Test
    fun closeAndJoinWaitsForOwnedWorkBeforeReturning() = runBlocking {
        val resources = ServerResources(HttpClientTimeoutPolicy(Millis(1_000), Millis(1_000), Millis(1_000)), Dispatchers.Default)
        val cancelled = resources.launchOwned { awaitCancellation() }

        resources.closeAndJoin()

        assertTrue(cancelled.isCompleted)
        assertEquals(0, resources.activeOwnedJobCount)
        assertEquals(false, resources.leakReport().hasLeaks)
        assertTrue(resources.isClosed)
    }
}
