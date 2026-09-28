package f95gm.server.upstream.request

import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.http.UpstreamRetryReason
import io.ktor.client.statement.HttpResponse

/** Typed boundary between retry execution and response processing. */
internal sealed interface RetryOutcome {
    val attempts: Int
    val reason: UpstreamRetryReason?

    data class Response(
        val response: HttpResponse,
        override val attempts: Int,
        override val reason: UpstreamRetryReason?
    ) : RetryOutcome

    data class Failure(
        val failure: UpstreamFailure,
        override val attempts: Int,
        override val reason: UpstreamRetryReason?
    ) : RetryOutcome
}
