package f95gm.server.upstream.request

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.net.URI

@JvmInline
internal value class UpstreamUri(val value: String) {
    init {
        val parsed = URI(value)
        require(parsed.isAbsolute && parsed.host != null && parsed.scheme in setOf("http", "https")) {
            "Upstream request URI must be an absolute HTTP(S) URI"
        }
    }
}

internal fun interface UpstreamHttpClient {
    suspend fun request(uri: UpstreamUri, configure: HttpRequestBuilder.() -> Unit): HttpResponse
}

internal class KtorUpstreamHttpClient(private val delegate: HttpClient) : UpstreamHttpClient {
    override suspend fun request(uri: UpstreamUri, configure: HttpRequestBuilder.() -> Unit): HttpResponse =
        delegate.request(uri.value, configure)
}
