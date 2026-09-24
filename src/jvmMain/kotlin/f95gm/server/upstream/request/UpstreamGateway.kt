package f95gm.server.upstream.request

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import io.ktor.http.*

internal data class UpstreamHeader(val name: String, val value: String)

internal class UpstreamHeaders private constructor(
    val values: List<UpstreamHeader>
) {
    companion object {
        val EMPTY = UpstreamHeaders(emptyList())

        fun of(values: Map<String, String>): UpstreamHeaders =
            UpstreamHeaders(values.map { (name, value) -> UpstreamHeader(name, value) })

        fun of(vararg values: UpstreamHeader): UpstreamHeaders = UpstreamHeaders(values.toList())
    }
}

internal interface UpstreamGateway {
    suspend fun request(
        method: HttpMethod,
        path: String,
        session: UpstreamSession,
        body: String? = null,
        contentType: String? = null,
        headers: UpstreamHeaders = UpstreamHeaders.EMPTY
    ): UpstreamResponse
}
