package f95gm.server.routes.item

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.upstream.mediaHosts
import f95gm.server.config.upstream.upstream
import f95gm.server.http.request.SafeHttpsUri
import f95gm.server.http.request.SafeHttpsUriResult
import f95gm.server.http.response.ServerMessageKeys
import f95gm.server.http.response.respondApiFailure
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.routeDependencies
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class MediaResponseTooLargeException : CancellationException("Media response exceeded configured limit")

internal suspend fun media(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToMedia()) ?: return
    val rawUrl = call.request.queryParameters["url"]
    val uri = rawUrl?.let {
        when (val result = SafeHttpsUri.parseResult(it, mediaHosts)) {
            is SafeHttpsUriResult.Valid -> result.uri
            is SafeHttpsUriResult.Invalid -> null
        }
    }
    if (uri == null || !call.routeDependencies.publicHostResolver.isFreshPublic(uri.host)) {
        respondApiFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            UiMessages.server_mediaUrlNotAllowed(),
            ServerMessageKeys.MEDIA_URL_NOT_ALLOWED
        )
        return
    }

    val response = call.routeDependencies.mediaFetcher.fetch(uri, session.header(), "$upstream/")
    if (response.statusCode !in 200..299) {
        response.close()
        respondApiFailure(
            call,
            HttpStatusCode(response.statusCode, "Upstream media"),
            UiMessages.server_mediaStatus(response.statusCode),
            ServerMessageKeys.MEDIA_STATUS
        )
        return
    }
    val responseContentLength = response.contentLength
    if (!mediaResponseWithinLimit(responseContentLength)) {
        response.close()
        respondApiFailure(
            call,
            ServerConstants.STATUS_BAD_GATEWAY,
            UiMessages.server_mediaStatus(ServerConstants.STATUS_PAYLOAD_TOO_LARGE.value),
            ServerMessageKeys.MEDIA_STATUS
        )
        return
    }
    val source = response.body
    val contentType = response.contentType?.let { raw ->
        when (val parsed = parseMediaContentType(raw)) {
            is MediaContentTypeResult.Valid -> parsed.value
            MediaContentTypeResult.Invalid -> null
        }
    }
    if (contentType != null && contentType.contentType != "image") {
        response.close()
        respondApiFailure(
            call,
            ServerConstants.STATUS_BAD_GATEWAY,
            UiMessages.server_mediaStatus(ServerConstants.STATUS_UNSUPPORTED_MEDIA_TYPE.value),
            ServerMessageKeys.MEDIA_STATUS
        )
        return
    }
    call.response.headers.append(HttpHeaders.ContentDisposition, "inline")
    call.respond(object : OutgoingContent.WriteChannelContent() {
        override val contentType = contentType
        override val contentLength = responseContentLength
        override suspend fun writeTo(channel: ByteWriteChannel) {
            val buffer = ByteArray(16 * 1024)
            var copied = 0L
            try {
                while (true) {
                    val read = withContext(Dispatchers.IO) { source.read(buffer) }
                    if (read == -1) break
                    if (read == 0) continue
                    copied += read
                    if (copied > ServerConstants.MAX_MEDIA_RESPONSE_BYTES) {
                        throw MediaResponseTooLargeException()
                    }
                    channel.writeFully(buffer, 0, read)
                }
            } finally {
                withContext(Dispatchers.IO) { response.close() }
            }
        }
    })
}

internal fun mediaResponseWithinLimit(contentLength: Long?): Boolean =
    contentLength == null || contentLength in 0..ServerConstants.MAX_MEDIA_RESPONSE_BYTES
