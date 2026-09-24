package f95gm.server.upstream.request

import f95gm.server.config.session.setCookiePairs
import f95gm.server.http.request.readLimitedBytes
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.model.http.upstreamResponseTooLargeResponse
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.IOException

internal class UpstreamResponseReader(
    private val maxResponseBytes: Long,
    private val persistSession: suspend (UpstreamSession) -> Unit
) {
    suspend fun read(response: HttpResponse, session: UpstreamSession): UpstreamResponse {
        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (contentLength != null && contentLength > maxResponseBytes) {
            response.bodyAsChannel().cancel(null)
            return tooLarge()
        }
        val body = try {
            response.bodyAsLimitedText(maxResponseBytes)
        } catch (_: IOException) {
            return tooLarge()
        }
        var cookiesChanged = false
        response.headers.getAll(HttpHeaders.SetCookie).orEmpty().forEach { headerValue ->
            setCookiePairs(headerValue).forEach { (name, cookieValue) ->
                if (session.cookieEntries()[name] != cookieValue) {
                    cookiesChanged = session.updateCookie(name, cookieValue) || cookiesChanged
                }
            }
        }
        if (cookiesChanged) persistSession(session)
        return UpstreamResponse.Success(
            response.status,
            body,
            response.headers[HttpHeaders.ContentType],
            response.headers[HttpHeaders.Location]
        )
    }

    private fun tooLarge() = upstreamResponseTooLargeResponse()
}

private suspend fun HttpResponse.bodyAsLimitedText(maxBytes: Long): String {
    val channel = bodyAsChannel()
    return try {
        channel.readLimitedBytes(maxBytes).decodeToString()
    } finally {
        channel.cancel(null)
    }
}
