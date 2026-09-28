package f95gm.server.upstream.request

import f95gm.server.config.session.setCookiePairs
import f95gm.server.config.Bytes
import f95gm.server.http.request.readLimitedBytes
import f95gm.server.http.request.parseContentTypeOrNull
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.model.http.UpstreamLocation
import f95gm.server.model.http.ResponseBodyBytes
import f95gm.server.model.http.upstreamResponseTooLargeResponse
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.IOException
import f95gm.domain.api.parseNonNegativeLong

internal class UpstreamResponseReader(
    private val responsePolicy: UpstreamResponsePolicy = UpstreamResponsePolicy(Bytes(4L * 1024L * 1024L)),
    private val persistSession: suspend (UpstreamSession) -> Unit = {}
) {
    internal constructor(maxResponseBytes: Bytes, persistSession: suspend (UpstreamSession) -> Unit = {}) :
        this(UpstreamResponsePolicy(maxResponseBytes), persistSession)
    /** Consumes and closes the response body, or releases it on an early failure. */
    suspend fun read(response: HttpResponse, session: UpstreamSession, policy: UpstreamResponsePolicy = responsePolicy): UpstreamResponse {
        val maxResponseBytes = policy.maxBodyBytes.value
        val contentLength = response.headers[HttpHeaders.ContentLength]?.parseNonNegativeLong()
        if (contentLength != null && contentLength > maxResponseBytes) {
            response.releaseUnconsumedBody()
            return tooLarge()
        }
        val bodyRead = when (val read = response.readLimitedBody(maxResponseBytes)) {
            is UpstreamBodyReadResult.Success -> read
            UpstreamBodyReadResult.TooLarge -> return tooLarge()
        }
        var cookiesChanged = false
        response.headers.getAll(HttpHeaders.SetCookie).orEmpty().forEach { headerValue ->
            setCookiePairs(headerValue).forEach { cookie ->
                if (session.cookieSnapshot().entries[UpstreamSession.CookieName(cookie.name)]?.value != cookie.value) {
                    cookiesChanged = session.updateCookie(cookie.name, cookie.value) || cookiesChanged
                }
            }
        }
        if (cookiesChanged) persistSession(session)
        return UpstreamResponse.Success(
            status = response.status,
            body = bodyRead.value,
            contentType = response.headers[HttpHeaders.ContentType]?.let(::parseContentTypeOrNull),
            location = response.headers[HttpHeaders.Location]?.takeIf(String::isNotBlank)?.let(::UpstreamLocation),
            bodyByteCount = ResponseBodyBytes(bodyRead.byteCount)
        )
    }

    private fun tooLarge() = upstreamResponseTooLargeResponse()
}

private sealed interface UpstreamBodyReadResult {
    data class Success(val value: String, val byteCount: Long) : UpstreamBodyReadResult
    data object TooLarge : UpstreamBodyReadResult
}

private suspend fun HttpResponse.readLimitedBody(maxBytes: Long): UpstreamBodyReadResult {
    val channel = bodyAsChannel()
    return try {
        channel.readLimitedBytes(maxBytes).let { bytes ->
            UpstreamBodyReadResult.Success(bytes.decodeToString(), bytes.size.toLong())
        }
    } catch (_: IOException) {
        UpstreamBodyReadResult.TooLarge
    } finally {
        channel.cancel(null)
    }
}
