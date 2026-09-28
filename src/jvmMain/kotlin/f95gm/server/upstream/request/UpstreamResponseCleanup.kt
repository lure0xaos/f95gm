package f95gm.server.upstream.request

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel

/**
 * Releases an upstream response body that will not be parsed by the caller.
 *
 * Callers must use this before retrying or returning early from a response path;
 * response readers own cleanup after they start consuming the body.
 */
internal suspend fun HttpResponse.releaseUnconsumedBody() {
    bodyAsChannel().cancel(null)
}
