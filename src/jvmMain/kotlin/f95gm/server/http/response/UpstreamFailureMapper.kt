package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.messages.UiMessages
import f95gm.server.model.http.UpstreamFailure

internal fun upstreamFailureApiFailure(failure: UpstreamFailure): ApiFailure = ApiFailure(
    message = UiMessages.server_upstreamFailure(),
    key = ServerMessageKeys.UPSTREAM_FAILURE,
    code = F95ApiErrorCode.UPSTREAM_FAILURE,
    details = ApiFailureDetails.reason(failure.name)
)
