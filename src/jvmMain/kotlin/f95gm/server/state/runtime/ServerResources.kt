package f95gm.server.state.runtime

import f95gm.server.upstream.request.KtorUpstreamHttpClient
import f95gm.server.upstream.request.UpstreamHttpClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.*

internal class ServerResources(
    timeouts: HttpClientTimeoutPolicy,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    client: HttpClient? = null
) : LifecycleResource {
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val closeGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed
    private val ownsClient = client == null
    val httpClient: HttpClient = client ?: HttpClient(CIO) {
        expectSuccess = false
        followRedirects = false
        install(HttpTimeout) {
            this.requestTimeoutMillis = timeouts.requestMillis.value
            this.connectTimeoutMillis = timeouts.connectMillis.value
            this.socketTimeoutMillis = timeouts.socketMillis.value
        }
    }
    val upstreamHttpClient: UpstreamHttpClient = KtorUpstreamHttpClient(httpClient)

    override fun close() = closeGuard.closeOnce {
        // Cancellation is cooperative; waiting here with runBlocking can deadlock when
        // close is initiated from one of the server scope's own threads.
        scope.cancel()
        if (ownsClient) httpClient.close()
    }
}
