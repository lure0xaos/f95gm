package f95gm.server.routes.item

import f95gm.messages.UiMessages
import f95gm.server.http.response.UpstreamStatusDisposition
import f95gm.server.http.response.upstreamDisposition
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ApiFailureResponse
import f95gm.server.http.response.ApiResponsePolicy
import f95gm.server.http.response.ServerMessageKey
import io.ktor.http.HttpStatusCode

internal fun itemFailureMessage(status: HttpStatusCode): String = when (status.upstreamDisposition) {
    UpstreamStatusDisposition.FORBIDDEN -> UiMessages.server_accessDenied()
    UpstreamStatusDisposition.NOT_FOUND -> UiMessages.server_itemNotFound()
    else -> UiMessages.server_itemLoadFailed(status.value)
}

private val itemResponsePolicy = ApiResponsePolicy<HttpStatusCode> { status -> ApiFailureResponse(
    status,
    ApiFailure(itemFailureMessage(status), ServerMessageKey.INVALID_ITEM_LOAD)
) }

internal fun itemFailureResponse(status: HttpStatusCode): ApiFailureResponse = itemResponsePolicy.failure(status)

