package f95gm.server.upstream.request

import io.ktor.http.HttpMethod
import kotlinx.coroutines.withTimeout

/** Executes one bounded transport attempt; retry outcome classification stays in the coordinator. */
internal class UpstreamRequestAttemptExecutor(
    private val transport: UpstreamTransport
) {
    suspend fun execute(
        method: HttpMethod,
        uri: UpstreamRequestUri,
        policy: UpstreamPolicy,
        context: UpstreamRequestContext
    ): RetryOutcome = withTimeout(policy.deadline.duration) {
        transport.requestWithRetry(method, uri, policy) {
            applyUpstreamTypedDefaults(
                method,
                context.session,
                context.headers,
                context.contentType,
                context.body?.text
            )
        }
    }
}
