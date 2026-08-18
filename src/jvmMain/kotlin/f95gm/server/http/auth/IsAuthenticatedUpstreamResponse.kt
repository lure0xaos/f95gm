package f95gm.server.http.auth

import f95gm.domain.api.F95HttpStatus
import f95gm.server.model.http.UpstreamResponse

internal fun isAuthenticatedUpstreamResponse(response: UpstreamResponse): Boolean =
    response.status in F95HttpStatus.SUCCESS_MIN..F95HttpStatus.SUCCESS_MAX && !isUnauthenticatedUpstreamBody(response.body)
