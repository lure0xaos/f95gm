package f95gm.server.upstream.media

import f95gm.server.application.media.*
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.model.http.HttpStatusValue
import java.io.IOException
import java.io.InputStream
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory
import f95gm.server.http.request.parseContentTypeOrNull

/** Narrow transport seam so media policy and response ownership can be tested without a live URL connection. */
internal interface MediaConnection : java.io.Closeable {
    val responseCode: Int
    val contentLength: Long
    val contentType: String?
    fun successBody(): InputStream
    fun errorBody(): InputStream?
}

internal fun interface MediaConnectionFactory {
    fun open(
        uri: URI,
        addresses: List<java.net.InetAddress>,
        cookie: MediaCookieHeader,
        referrer: MediaReferrer,
        connectTimeoutMillis: Int,
        readTimeoutMillis: Int
    ): MediaConnection
}

internal class PinnedMediaConnectionFactory : MediaConnectionFactory {
    override fun open(
        uri: URI,
        addresses: List<java.net.InetAddress>,
        cookie: MediaCookieHeader,
        referrer: MediaReferrer,
        connectTimeoutMillis: Int,
        readTimeoutMillis: Int
    ): MediaConnection {
        val connection = (uri.toURL().openConnection(java.net.Proxy.NO_PROXY) as HttpsURLConnection).apply {
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("User-Agent", UpstreamProtocolConfig.USER_AGENT)
            setRequestProperty("Cookie", cookie.value)
            setRequestProperty("Referer", referrer.value)
            setRequestProperty("Accept", UpstreamProtocolConfig.ACCEPT_MEDIA)
            sslSocketFactory = PinnedSslSocketFactory(
                defaultSSLSocketFactory,
                uri.host,
                uri.port.takeIf { it > 0 } ?: 443,
                addresses,
                connectTimeoutMillis
            )
        }
        return HttpsMediaConnection(connection)
    }

    private class HttpsMediaConnection(private val delegate: HttpsURLConnection) : MediaConnection {
        override val responseCode: Int get() = delegate.responseCode
        override val contentLength: Long get() = delegate.contentLengthLong
        override val contentType: String? get() = delegate.contentType
        override fun successBody(): InputStream = delegate.inputStream
        override fun errorBody(): InputStream? = delegate.errorStream
        override fun close() = delegate.disconnect()
    }

    private companion object {
        val defaultSSLSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    }
}

/**
 * Opens media through the address set resolved immediately before connecting.
 * The URL hostname remains intact, so HTTPS SNI and certificate hostname checks
 * still use the allow-listed hostname while the TCP socket is pinned to the
 * validated address.
 */
internal class PinnedMediaFetcher(
    private val hostResolver: PublicHostResolverPort,
    private val connectionFactory: MediaConnectionFactory = PinnedMediaConnectionFactory(),
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 30_000
) : MediaFetcher {
    override fun fetchResult(uri: URI, cookie: MediaCookieHeader, referrer: MediaReferrer): MediaFetchAttempt {
        if (!uri.scheme.equals("https", ignoreCase = true)) return MediaFetchAttempt.Rejected(MediaFetchRejection.UNSAFE_SCHEME)
        val addresses = hostResolver.freshPublicAddresses(HostName(uri.host))
        if (addresses.isEmpty()) return MediaFetchAttempt.Rejected(MediaFetchRejection.NO_PUBLIC_ADDRESS)
        val connection = try {
            connectionFactory.open(uri, addresses, cookie, referrer, connectTimeoutMillis, readTimeoutMillis)
        } catch (error: IOException) {
            return MediaFetchAttempt.Rejected(MediaFetchRejection.IO_FAILURE, error)
        } catch (error: IllegalArgumentException) {
            return MediaFetchAttempt.Rejected(MediaFetchRejection.INVALID_RESPONSE, error)
        } catch (error: ClassCastException) {
            return MediaFetchAttempt.Rejected(MediaFetchRejection.INVALID_RESPONSE, error)
        }
        var ownershipTransferred = false
        return try {
            val status = connection.responseCode
            val body = if (status in 200..299) connection.successBody() else connection.errorBody()
                ?: InputStream.nullInputStream()
            val response = MediaFetchResponse(
                HttpStatusValue(status),
                connection.contentLength.takeIf { it >= 0 }?.let(::ContentLength),
                connection.contentType?.let(::parseContentTypeOrNull),
                BinaryBody(body, connection::close)
            )
            ownershipTransferred = true
            MediaFetchAttempt.Fetched(response)
        } catch (error: IOException) {
            MediaFetchAttempt.Rejected(MediaFetchRejection.IO_FAILURE, error)
        } catch (error: IllegalArgumentException) {
            MediaFetchAttempt.Rejected(MediaFetchRejection.INVALID_RESPONSE, error)
        } finally {
            if (!ownershipTransferred) connection.close()
        }
    }
}
