package f95gm.server.application.media

import f95gm.server.model.http.HttpStatusValue
import io.ktor.http.ContentType
import java.io.Closeable
import java.io.InputStream
import java.net.InetAddress

@JvmInline
internal value class ContentLength(val value: Long) {
    init { require(value >= 0L) }
}

@JvmInline
internal value class MediaCookieHeader(val value: String)

@JvmInline
internal value class MediaReferrer(val value: String)

@JvmInline
internal value class HostName(val value: String) {
    init { require(value.isNotBlank()) }
}

internal class BinaryBody(
    private val source: InputStream,
    private val onClose: () -> Unit = {}
) : Closeable {
    private var closed = false

    fun read(buffer: ByteArray): Int = source.read(buffer)
    override fun close() {
        if (closed) return
        closed = true
        try {
            source.close()
        } finally {
            onClose()
        }
    }
}

internal data class MediaFetchResponse(
    val statusCode: HttpStatusValue,
    val contentLength: ContentLength?,
    val contentType: ContentType?,
    val body: BinaryBody
) : Closeable {
    override fun close() = body.close()
}

internal fun interface MediaFetcher {
    fun fetchResult(uri: java.net.URI, cookie: MediaCookieHeader, referrer: MediaReferrer): MediaFetchAttempt
}

internal sealed interface MediaFetchAttempt {
    data class Fetched(val response: MediaFetchResponse) : MediaFetchAttempt
    data class Rejected(val reason: MediaFetchRejection, val cause: Throwable? = null) : MediaFetchAttempt
}

internal enum class MediaFetchRejection { UNSAFE_SCHEME, NO_PUBLIC_ADDRESS, IO_FAILURE, INVALID_RESPONSE }

/** Application-facing DNS policy seam; the resolver implementation belongs to upstream transport. */
internal interface PublicHostResolverPort {
    fun isFreshPublic(host: HostName): Boolean
    fun freshPublicAddresses(host: HostName): List<InetAddress>
}
