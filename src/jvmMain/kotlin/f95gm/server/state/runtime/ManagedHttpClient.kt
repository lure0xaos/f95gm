package f95gm.server.state.runtime

import f95gm.server.upstream.request.KtorUpstreamHttpClient
import f95gm.server.upstream.request.UpstreamHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource

/** Owns HTTP-client construction and ownership; lifecycle code only closes this resource. */
internal class ManagedHttpClient(
    timeouts: HttpClientTimeoutPolicy,
    dependency: HttpClientDependency
) : LifecycleResource {
    private val closeGuard = CloseGuard()
    private val ownsClient = dependency.ownership == DependencyOwnership.OWNED
    val client: HttpClient = when (dependency) {
        is HttpClientDependency.Provided -> dependency.value
        HttpClientDependency.Default -> HttpClient(CIO) {
            expectSuccess = false
            followRedirects = false
            install(HttpTimeout) {
                requestTimeoutMillis = timeouts.requestMillis.value
                connectTimeoutMillis = timeouts.connectMillis.value
                socketTimeoutMillis = timeouts.socketMillis.value
            }
        }
    }
    val upstream: UpstreamHttpClient = KtorUpstreamHttpClient(client)

    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce {
        if (ownsClient) client.close()
    }
}
