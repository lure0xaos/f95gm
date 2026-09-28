package f95gm.server.upstream.request

import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.http.UpstreamResponse

internal fun transportFailureResponse(): UpstreamResponse.Failure = UpstreamResponse.Failure(
    status = HttpStatusCatalog.BAD_GATEWAY,
    body = "",
    contentType = null,
    reason = UpstreamFailure.CONNECTION_FAILURE
)

internal fun timeoutFailureResponse(): UpstreamResponse.Failure = UpstreamResponse.Failure(
    status = HttpStatusCatalog.GATEWAY_TIMEOUT,
    body = "",
    contentType = null,
    reason = UpstreamFailure.TIMEOUT
)
