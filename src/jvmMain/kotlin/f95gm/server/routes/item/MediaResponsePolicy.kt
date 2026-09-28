package f95gm.server.routes.item

import f95gm.messages.UiMessages
import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ApiFailureResponse
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.http.response.ApiResponsePolicy
import io.ktor.http.HttpStatusCode

private val mediaStatusPolicy = ApiResponsePolicy<Int> { status -> ApiFailureResponse(
    HttpStatusCode(status, "Upstream media"),
    ApiFailure(UiMessages.server_mediaStatus(status), ServerMessageKey.MEDIA_STATUS)
) }

internal fun mediaStatusFailure(status: Int): ApiFailureResponse = mediaStatusPolicy.failure(status)

internal fun mediaTooLargeFailure(): ApiFailureResponse = ApiFailureResponse(
    HttpStatusCatalog.BAD_GATEWAY,
    ApiFailure(
        UiMessages.server_mediaStatus(HttpStatusCatalog.PAYLOAD_TOO_LARGE.value),
        ServerMessageKey.MEDIA_STATUS
    )
)

internal fun mediaTypeFailure(): ApiFailureResponse = ApiFailureResponse(
    HttpStatusCatalog.BAD_GATEWAY,
    ApiFailure(
        UiMessages.server_mediaStatus(HttpStatusCatalog.UNSUPPORTED_MEDIA_TYPE.value),
        ServerMessageKey.MEDIA_STATUS
    )
)

