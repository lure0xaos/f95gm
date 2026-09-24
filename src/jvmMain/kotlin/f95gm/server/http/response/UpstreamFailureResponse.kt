package f95gm.server.http.response

import f95gm.server.model.http.UpstreamResponse
import io.ktor.server.application.*

/** Converts all typed upstream failures to the same localized API envelope. */
internal suspend fun ApplicationCall.respondUpstreamFailure(response: UpstreamResponse): Boolean {
    return when (response) {
        is UpstreamResponse.Success -> false
        is UpstreamResponse.Failure -> {
            respondFailure(this, response.status, upstreamFailureApiFailure(response.reason))
            true
        }
    }
}
