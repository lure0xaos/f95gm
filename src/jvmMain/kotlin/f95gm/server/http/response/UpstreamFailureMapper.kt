package f95gm.server.http.response

import f95gm.server.model.http.UpstreamFailure

internal fun upstreamFailureApiFailure(failure: UpstreamFailure): ApiFailure = ApiFailureAdapters.upstream(failure)

