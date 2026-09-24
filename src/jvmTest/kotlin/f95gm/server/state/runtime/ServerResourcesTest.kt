package f95gm.server.state.runtime

import f95gm.server.config.Millis
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerResourcesTest {
    @Test
    fun closeCancelsScopeWithoutClosingAnInjectedClient() = runBlocking {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val resources =
            ServerResources(HttpClientTimeoutPolicy(Millis(1_000), Millis(1_000), Millis(1_000)), client = client)
        val started = CompletableDeferred<Unit>()
        val job = resources.scope.launch {
            started.complete(Unit)
            awaitCancellation()
        }
        started.await()

        resources.close()
        job.join()

        assertTrue(resources.isClosed)
        assertTrue(job.isCancelled)
        assertEquals("ok", client.get("https://fixture.test/").bodyAsText())
        client.close()
    }
}
