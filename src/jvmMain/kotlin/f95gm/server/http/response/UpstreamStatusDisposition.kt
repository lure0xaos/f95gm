package f95gm.server.http.response

import io.ktor.http.HttpStatusCode

internal enum class UpstreamStatusDisposition {
    SUCCESS,
    FORBIDDEN,
    NOT_FOUND,
    FAILURE
}

internal val HttpStatusCode.upstreamDisposition: UpstreamStatusDisposition
    get() = when {
        isSuccessful -> UpstreamStatusDisposition.SUCCESS
        this == HttpStatusCode.Forbidden -> UpstreamStatusDisposition.FORBIDDEN
        this == HttpStatusCode.NotFound -> UpstreamStatusDisposition.NOT_FOUND
        else -> UpstreamStatusDisposition.FAILURE
    }
