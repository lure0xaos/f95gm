package f95gm.server.routes.item

import f95gm.messages.UiMessages
import f95gm.server.application.media.MediaFetchResult
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.http.response.respondApiFailure
import f95gm.server.http.response.respondFailure
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.routes.item.mediaDependencies
import f95gm.server.model.media.MediaUrl
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import f95gm.domain.api.F95ApiQueryParameters
import f95gm.server.http.request.get
import f95gm.server.config.constants.HttpStatusCatalog

internal suspend fun media(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToMedia()) ?: return
    val dependencies = call.mediaDependencies
    val result = dependencies.mediaService.fetch(
        call.request.queryParameters[F95ApiQueryParameters.URL]?.let(::MediaUrl), session.cookieSnapshot().header
    )
    val response = when (result) {
        is MediaFetchResult.NotAllowed -> {
            respondApiFailure(
                call, HttpStatusCatalog.BAD_REQUEST, UiMessages.server_mediaUrlNotAllowed(),
                ServerMessageKey.MEDIA_URL_NOT_ALLOWED
            )
            return
        }
        is MediaFetchResult.Fetched -> result.response
    }
    val policy = dependencies.mediaResourcePolicy
    val decision = policy.evaluate(response)
    if (decision != MediaResponseDecision.ACCEPT) {
        response.close()
        val failure = when (decision) {
            MediaResponseDecision.UPSTREAM_STATUS -> mediaStatusFailure(response.statusCode.value)
            MediaResponseDecision.TOO_LARGE -> mediaTooLargeFailure()
            MediaResponseDecision.INVALID_CONTENT_TYPE -> mediaTypeFailure()
            MediaResponseDecision.ACCEPT -> error("Accepted media cannot be rejected")
        }
        respondFailure(call, failure)
        return
    }
    call.response.headers.append(HttpHeaders.ContentDisposition, mediaContentDisposition(response.contentType))
    call.respond(object : OutgoingContent.WriteChannelContent() {
        override val contentType = response.contentType
        override val contentLength = response.contentLength?.value
        override suspend fun writeTo(channel: ByteWriteChannel) {
            streamMediaResponse(response, policy, dependencies.ioDispatcher) { buffer, read ->
                channel.writeFully(buffer, 0, read)
            }
        }
    })
}

internal fun mediaContentDisposition(contentType: ContentType?): String =
    if (contentType?.contentType == "image" && contentType.contentSubtype.equals("svg+xml", ignoreCase = true)) {
        "attachment"
    } else {
        "inline"
    }

