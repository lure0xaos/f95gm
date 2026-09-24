package f95gm.server.model.http

import f95gm.server.config.constants.ServerConstants
import io.ktor.http.*

internal sealed interface UpstreamResponse {
    val status: HttpStatusCode
    val body: String
    val contentType: String?
    val location: String?
    val retryAttempts: Int
    val retryReason: UpstreamRetryReason?

    data class Success(
        override val status: HttpStatusCode,
        override val body: String,
        override val contentType: String?,
        override val location: String? = null,
        override val retryAttempts: Int = 1,
        override val retryReason: UpstreamRetryReason? = null
    ) : UpstreamResponse

    data class Failure(
        override val status: HttpStatusCode,
        override val body: String,
        override val contentType: String?,
        override val location: String? = null,
        val reason: UpstreamFailure
    ) : UpstreamResponse {
        override val retryAttempts: Int = 1
        override val retryReason: UpstreamRetryReason? = null
    }
}

internal fun upstreamCircuitOpenResponse(): UpstreamResponse =
    UpstreamResponse.Failure(
        status = ServerConstants.STATUS_SERVICE_UNAVAILABLE,
        body = "",
        contentType = null,
        reason = UpstreamFailure.CIRCUIT_OPEN
    )

internal fun upstreamResponseTooLargeResponse(): UpstreamResponse =
    UpstreamResponse.Failure(
        status = ServerConstants.STATUS_PAYLOAD_TOO_LARGE,
        body = "",
        contentType = null,
        reason = UpstreamFailure.RESPONSE_TOO_LARGE
    )

internal enum class UpstreamFailure {
    CIRCUIT_OPEN("The upstream service is temporarily unavailable"),
    RESPONSE_TOO_LARGE("The upstream response was too large"),
    TRANSPORT("The upstream service could not be reached"),
    TIMEOUT("The upstream service timed out");

    val userMessage: String

    private constructor(userMessage: String) {
        this.userMessage = userMessage
    }
}

internal val UpstreamResponse.userFacingBody: String
    get() = when (this) {
        is UpstreamResponse.Success -> body
        is UpstreamResponse.Failure -> reason.userMessage
    }

internal enum class UpstreamRetryReason {
    HTTP_STATUS,
    IO_EXCEPTION
}
