package f95gm.server.routes.item

import f95gm.messages.UiMessages
import f95gm.server.observability.logger
import f95gm.server.http.request.positiveThreadIdParameter
import f95gm.server.http.response.*
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.routes.item.itemDependencies
import f95gm.server.model.ParseResult
import f95gm.server.upstream.parser.parseThreadDetailResult
import io.ktor.http.*
import io.ktor.server.application.*
import org.jsoup.Jsoup
import f95gm.server.config.constants.HttpStatusCatalog

internal suspend fun item(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToItem()) ?: return
    val dependencies = call.itemDependencies
    val threadId = call.positiveThreadIdParameter("threadId")
    if (threadId == null) {
        respondApiFailure(
            call,
            HttpStatusCatalog.BAD_REQUEST,
            UiMessages.server_invalidItemId(),
            ServerMessageKey.INVALID_ITEM_ID
        )
        return
    }

    val result = dependencies.itemService.fetch(threadId, session)
    if (call.respondUpstreamFailure(result)) return
    if (!result.status.isSuccessful) {
        logger.warn {
            "F95zone item request failed: threadId=$threadId, status=${result.status}, " +
                    "location=${result.location?.value ?: "none"}, contentType=${result.contentType ?: "none"}, " +
                    "bodyLength=${result.body.length}, title=${
                        Jsoup.parse(result.body, dependencies.upstreamEndpoints.baseUrl.value).title().ifBlank { "none" }
                    }"
        }
        respondFailure(call, itemFailureResponse(result.status))
        return
    }

    when (val parsed = parseThreadDetailResult(threadId, result.body, dependencies.upstreamEndpoints.baseUrl)) {
        is ParseResult.Success -> respondJson(call, HttpStatusCatalog.OK, parsed.value)
        is ParseResult.Invalid -> respondApiFailure(
            call,
            HttpStatusCatalog.BAD_GATEWAY,
            UiMessages.server_itemLoadFailed(HttpStatusCatalog.BAD_GATEWAY.value),
            ServerMessageKey.INVALID_ITEM_LOAD
        )
    }
}

