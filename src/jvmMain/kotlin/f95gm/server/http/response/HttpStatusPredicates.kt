package f95gm.server.http.response

import io.ktor.http.*

internal val HttpStatusCode.isSuccessful: Boolean
    get() = value in 200..299

internal val HttpStatusCode.isRedirect: Boolean
    get() = value in 300..399

internal val HttpStatusCode.isClientError: Boolean
    get() = value in 400..499
