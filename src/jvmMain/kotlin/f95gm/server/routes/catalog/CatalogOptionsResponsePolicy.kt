package f95gm.server.routes.catalog

import f95gm.messages.UiMessages
import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.http.response.ApiFailureResponse
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.kernel.ServerInvariantViolation


internal fun CatalogOptionsResult.failureResponse(): ApiFailureResponse = when (this) {
    is CatalogOptionsResult.UpstreamFailure -> throw ServerInvariantViolation(
        "Upstream failures must use the upstream response policy"
    )
    CatalogOptionsResult.Unauthenticated -> ApiFailureResponse(
        HttpStatusCatalog.UNAUTHORIZED,
        ApiFailure(UiMessages.server_signInAgain(), ServerMessageKey.SIGN_IN_AGAIN)
    )
    is CatalogOptionsResult.Invalid -> ApiFailureResponse(
        HttpStatusCatalog.BAD_GATEWAY,
        ApiFailure(UiMessages.server_catalogOptionsInvalid(), ServerMessageKey.CATALOG_OPTIONS_INVALID)
    )
    is CatalogOptionsResult.Success -> throw ServerInvariantViolation(
        "Successful catalog results have no failure response"
    )
}

internal fun catalogUpstreamFallbackResponse(response: UpstreamResponse): ApiFailureResponse =
    ApiFailureResponse(
        response.status,
        ApiFailure(UiMessages.server_catalogOptionsMissing(), ServerMessageKey.CATALOG_OPTIONS_MISSING)
    )

