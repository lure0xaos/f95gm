package f95gm.server.routes.item

import f95gm.server.application.media.ContentLength
import f95gm.server.application.media.MediaFetchResponse
import f95gm.server.routes.ports.MediaResourcePolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import f95gm.server.config.Bytes

internal class MediaResponseTooLargeException : CancellationException("Media response exceeded configured limit")

internal enum class MediaResponseDecision { ACCEPT, UPSTREAM_STATUS, TOO_LARGE, INVALID_CONTENT_TYPE }

/** Unknown lengths are checked while streaming; only declared image content is served inline. */
internal fun MediaResourcePolicy.evaluate(response: MediaFetchResponse): MediaResponseDecision = when {
    response.statusCode.value !in 200..299 -> MediaResponseDecision.UPSTREAM_STATUS
    !mediaResponseWithinLimit(response.contentLength, maxResponseBytes) -> MediaResponseDecision.TOO_LARGE
    response.contentType?.contentType != "image" -> MediaResponseDecision.INVALID_CONTENT_TYPE
    else -> MediaResponseDecision.ACCEPT
}

/** Owns the upstream body through success, overflow, downstream failure, and cancellation. */
internal suspend fun streamMediaResponse(
    response: MediaFetchResponse,
    policy: MediaResourcePolicy,
    ioDispatcher: CoroutineDispatcher,
    write: suspend (ByteArray, Int) -> Unit
) {
    try {
        val buffer = ByteArray(policy.downloadBufferBytes.value)
        var copied = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = withContext(ioDispatcher) { response.body.read(buffer) }
            if (read == -1) break
            if (read > 0) {
                if (read.toLong() > policy.maxResponseBytes.value - copied) throw MediaResponseTooLargeException()
                copied += read
                write(buffer, read)
            }
        }
    } finally {
        withContext(NonCancellable + ioDispatcher) { response.close() }
    }
}

internal fun mediaResponseWithinLimit(contentLength: ContentLength?, maxResponseBytes: Bytes): Boolean =
    contentLength == null || contentLength.value <= maxResponseBytes.value
