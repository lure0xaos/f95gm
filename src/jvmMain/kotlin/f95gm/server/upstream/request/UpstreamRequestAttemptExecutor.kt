package f95gm.server.upstream.request

import io.ktor.http.HttpMethod
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import java.io.IOException

internal class UpstreamRequestDeadlineExceeded : IOException("Upstream request deadline exceeded")

/** Executes one bounded transport attempt; retry outcome classification stays in the coordinator. */
internal class UpstreamRequestAttemptExecutor(
    private val transport: UpstreamTransport
) {
    suspend fun execute(
        method: HttpMethod,
        uri: UpstreamRequestUri,
        policy: UpstreamPolicy,
        context: UpstreamRequestContext
    ): RetryOutcome = try {
        withTimeout(policy.deadline.duration) {
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
    } catch (timeout: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        throw UpstreamRequestDeadlineExceeded().also { it.initCause(timeout) }
    }
}
