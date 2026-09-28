package f95gm.server.upstream.media

import f95gm.server.application.media.*
import f95gm.server.model.http.HttpStatusValue
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PinnedMediaFetcherTest {
    @Test
    fun injectsTheConnectionFactoryAndClosesTheConnectionWithTheBody() {
        val address = InetAddress.getByAddress(byteArrayOf(192.toByte(), 0, 2, 1))
        var opened: MediaConnectionRequest? = null
        val connection = FakeMediaConnection(200, ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        val fetcher = PinnedMediaFetcher(
            hostResolver = resolver(address),
            connectionFactory = MediaConnectionFactory { uri, addresses, cookie, referrer, connectTimeout, readTimeout ->
                opened = MediaConnectionRequest(uri, addresses, cookie, referrer, connectTimeout, readTimeout)
                connection
            },
            connectTimeoutMillis = 111,
            readTimeoutMillis = 222
        )

        val result = assertIs<MediaFetchAttempt.Fetched>(
            fetcher.fetchResult(
                URI("https://attachments.f95zone.to/file.png"),
                MediaCookieHeader("cookie"),
                MediaReferrer("https://f95zone.to/")
            )
        )

        assertEquals(address, opened?.addresses?.single())
        assertEquals("cookie", opened?.cookie?.value)
        assertEquals("https://f95zone.to/", opened?.referrer?.value)
        assertEquals(111, opened?.connectTimeout)
        assertEquals(222, opened?.readTimeout)
        result.response.close()
        assertTrue(connection.closed)
    }

    @Test
    fun usesTheErrorBodyForNonSuccessfulResponses() {
        val connection = FakeMediaConnection(
            responseCode = 404,
            success = ByteArrayInputStream(byteArrayOf(1)),
            error = ByteArrayInputStream(byteArrayOf(9))
        )
        val result = assertIs<MediaFetchAttempt.Fetched>(
            PinnedMediaFetcher(
                hostResolver = resolver(),
                connectionFactory = MediaConnectionFactory { _, _, _, _, _, _ -> connection }
            ).fetchResult(
                URI("https://attachments.f95zone.to/file.png"),
                MediaCookieHeader(""),
                MediaReferrer("")
            )
        )

        val buffer = ByteArray(1)
        assertEquals(1, result.response.body.read(buffer))
        assertEquals(9, buffer.single())
        result.response.close()
        assertTrue(connection.closed)
    }

    @Test
    fun closesConnectionWhenUnexpectedMetadataFailureEscapes() {
        var closed = false
        val connection = object : MediaConnection {
            override val responseCode = 200
            override val contentLength: Long get() = error("unexpected metadata failure")
            override val contentType: String? = "image/png"
            override fun successBody(): InputStream = ByteArrayInputStream(byteArrayOf(1))
            override fun errorBody(): InputStream? = null
            override fun close() { closed = true }
        }
        val fetcher = PinnedMediaFetcher(
            hostResolver = resolver(),
            connectionFactory = MediaConnectionFactory { _, _, _, _, _, _ -> connection }
        )

        assertFailsWith<IllegalStateException> {
            fetcher.fetchResult(
                URI("https://attachments.f95zone.to/file.png"),
                MediaCookieHeader(""),
                MediaReferrer("")
            )
        }

        assertTrue(closed)
    }

    private fun resolver(address: InetAddress = InetAddress.getByAddress(byteArrayOf(192.toByte(), 0, 2, 2))) =
        object : PublicHostResolverPort {
            override fun isFreshPublic(host: HostName): Boolean = true
            override fun freshPublicAddresses(host: HostName): List<InetAddress> = listOf(address)
        }

    private data class MediaConnectionRequest(
        val uri: URI,
        val addresses: List<InetAddress>,
        val cookie: MediaCookieHeader,
        val referrer: MediaReferrer,
        val connectTimeout: Int,
        val readTimeout: Int
    )

    private class FakeMediaConnection(
        override val responseCode: Int,
        private val success: InputStream,
        private val error: InputStream = ByteArrayInputStream(byteArrayOf())
    ) : MediaConnection {
        override val contentLength: Long = -1
        override val contentType: String? = "image/png"
        var closed = false

        override fun successBody(): InputStream = success
        override fun errorBody(): InputStream = error
        override fun close() { closed = true }
    }
}
