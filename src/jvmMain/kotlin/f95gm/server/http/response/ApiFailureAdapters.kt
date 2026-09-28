package f95gm.server.http.response

import f95gm.server.model.http.UpstreamFailure

/** Named adapters keep infrastructure failures from leaking into wire response construction. */
internal object ApiFailureAdapters {
    fun upstream(failure: UpstreamFailure): ApiFailure = ApiFailureCatalog.upstream(failure)
}
