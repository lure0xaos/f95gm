package f95gm.server.model.http

import f95gm.server.config.constants.HttpStatusCatalog
import io.ktor.http.*

@JvmInline
internal value class UpstreamLocation(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
internal value class ResponseBodyBytes(val value: Long) {
    init { require(value >= 0L) }
}

internal sealed interface UpstreamResponse {
    val status: HttpStatusCode
    val body: String
    val bodyByteCount: ResponseBodyBytes
    val contentType: ContentType?
    val location: UpstreamLocation?
    val retryAttempts: Int
    val retryReason: UpstreamRetryReason?

    data class Success(
        override val status: HttpStatusCode,
        override val body: String,
        override val contentType: ContentType?,
        override val location: UpstreamLocation? = null,
        override val retryAttempts: Int = 1,
        override val retryReason: UpstreamRetryReason? = null,
        override val bodyByteCount: ResponseBodyBytes = ResponseBodyBytes(body.encodeToByteArray().size.toLong())
    ) : UpstreamResponse

    data class Failure(
        override val status: HttpStatusCode,
        override val body: String,
        override val contentType: ContentType?,
        override val location: UpstreamLocation? = null,
        val reason: UpstreamFailure,
        override val bodyByteCount: ResponseBodyBytes = ResponseBodyBytes(body.encodeToByteArray().size.toLong())
    ) : UpstreamResponse {
        override val retryAttempts: Int = 1
        override val retryReason: UpstreamRetryReason? = null
    }
}

internal fun upstreamCircuitOpenResponse(): UpstreamResponse =
    UpstreamResponse.Failure(
        status = HttpStatusCatalog.SERVICE_UNAVAILABLE,
        body = "",
        contentType = null,
        reason = UpstreamFailure.CIRCUIT_OPEN
    )

internal fun upstreamResponseTooLargeResponse(): UpstreamResponse =
    UpstreamResponse.Failure(
        status = HttpStatusCatalog.PAYLOAD_TOO_LARGE,
        body = "",
        contentType = null,
        reason = UpstreamFailure.RESPONSE_TOO_LARGE
    )

internal val UpstreamResponse.userFacingBody: String
    get() = when (this) {
        is UpstreamResponse.Success -> body
        is UpstreamResponse.Failure -> reason.userMessage
    }

internal enum class UpstreamRetryReason {
    HTTP_STATUS,
    IO_EXCEPTION
}
