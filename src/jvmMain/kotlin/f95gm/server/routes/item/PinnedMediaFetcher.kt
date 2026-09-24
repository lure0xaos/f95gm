package f95gm.server.routes.item

import f95gm.server.config.upstream.upstreamUserAgent
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.Proxy
import java.net.Socket
import java.net.URI
import javax.net.SocketFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

internal data class MediaFetchResponse(
    val statusCode: Int,
    val contentLength: Long?,
    val contentType: String?,
    val body: InputStream
) : Closeable {
    override fun close() = body.close()
}

internal interface MediaFetcher {
    fun fetch(uri: URI, cookie: String, referrer: String): MediaFetchResponse
}

/**
 * Opens media through the address set resolved immediately before connecting.
 * The URL hostname remains intact, so HTTPS SNI and certificate hostname checks
 * still use the allow-listed hostname while the TCP socket is pinned to the
 * validated address.
 */
internal class PinnedMediaFetcher(
    private val hostResolver: PublicHostResolver,
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 30_000
) : MediaFetcher {
    override fun fetch(uri: URI, cookie: String, referrer: String): MediaFetchResponse {
        require(uri.scheme.equals("https", ignoreCase = true)) { "Media must use HTTPS" }
        val addresses = hostResolver.freshPublicAddresses(uri.host)
        require(addresses.isNotEmpty()) { "Media host did not resolve to a public address" }
        val connection = (uri.toURL().openConnection(Proxy.NO_PROXY) as HttpsURLConnection).apply {
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("User-Agent", upstreamUserAgent)
            setRequestProperty("Cookie", cookie)
            setRequestProperty("Referer", referrer)
            setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            sslSocketFactory =
                PinnedSslSocketFactory(defaultSSLSocketFactory, uri.host, uri.port.takeIf { it > 0 } ?: 443, addresses)
        }
        return try {
            val status = connection.responseCode
            val body = if (status in 200..299) connection.inputStream else connection.errorStream
                ?: InputStream.nullInputStream()
            MediaFetchResponse(status, connection.contentLengthLong.takeIf { it >= 0 }, connection.contentType, body)
        } catch (error: IOException) {
            connection.disconnect()
            throw error
        } catch (error: RuntimeException) {
            connection.disconnect()
            throw error
        }
    }

    private companion object {
        val defaultSSLSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    }
}

private class PinnedSslSocketFactory(
    private val delegate: SSLSocketFactory,
    private val host: String,
    private val port: Int,
    private val addresses: List<InetAddress>
) : SSLSocketFactory() {
    private fun pinnedSocket(): Socket = SocketFactory.getDefault().createSocket(addresses.first(), port)

    override fun createSocket(host: String, port: Int): Socket =
        delegate.createSocket(this.pinnedSocket(), this.host, this.port, true)

    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
        delegate.createSocket(this.pinnedSocket(), this.host, this.port, true)

    override fun createSocket(address: InetAddress, port: Int): Socket =
        delegate.createSocket(this.pinnedSocket(), this.host, this.port, true)

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        delegate.createSocket(this.pinnedSocket(), this.host, this.port, true)

    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket =
        delegate.createSocket(socket, this.host, this.port, autoClose)

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
}
