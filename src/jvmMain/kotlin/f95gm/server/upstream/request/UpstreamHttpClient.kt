package f95gm.server.upstream.request

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

internal fun interface UpstreamHttpClient {
    suspend fun request(url: String, configure: HttpRequestBuilder.() -> Unit): HttpResponse
}

internal class KtorUpstreamHttpClient(private val delegate: HttpClient) : UpstreamHttpClient {
    override suspend fun request(url: String, configure: HttpRequestBuilder.() -> Unit): HttpResponse =
        delegate.request(url, configure)
}
