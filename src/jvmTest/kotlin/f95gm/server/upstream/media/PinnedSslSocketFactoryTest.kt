package f95gm.server.upstream.media

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PinnedSslSocketFactoryTest {
    private val addresses = listOf(
        InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)),
        InetAddress.getByAddress(byteArrayOf(127, 0, 0, 2))
    )

    @Test
    fun pinnedConnectorRetriesValidatedAddressesAndReturnsFirstSuccess() {
        val attempts = mutableListOf<InetAddress>()
        val socket = Socket()
        val connector = PinnedAddressConnector { address, port, timeout ->
            attempts += address
            assertEquals(443, port)
            assertEquals(250, timeout)
            if (address == addresses.first()) throw IOException("connection refused")
            socket
        }

        assertSame(socket, connector.connect(addresses, 443, 250))
        assertEquals(addresses, attempts)
    }

    @Test
    fun pinnedConnectorReportsEmptyAddressListAndLastConnectionFailure() {
        assertEquals(
            "No validated media address was available",
            assertFailsWith<IOException> { PinnedAddressConnector { _, _, _ -> error("not called") }.connect(emptyList(), 443, 1) }.message
        )
        val failure = IOException("last failure")
        assertSame(
            failure,
            assertFailsWith<IOException> {
                PinnedAddressConnector { _, _, _ -> throw failure }.connect(addresses, 443, 1)
            }
        )
    }

    @Test
    fun tlsFactoryUsesPinnedSocketButKeepsConfiguredHostnameForVerification() {
        val rawSocket = Socket()
        var connectedAddress: InetAddress? = null
        var wrappedSocket: Socket? = null
        var wrappedHost: String? = null
        var wrappedPort = 0
        var wrappedAutoClose = false
        val delegate = object : SSLSocketFactory() {
            override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
                wrappedSocket = socket
                wrappedHost = host
                wrappedPort = port
                wrappedAutoClose = autoClose
                return socket
            }
            override fun createSocket(): Socket = error("unused")
            override fun createSocket(host: String, port: Int): Socket = error("unused")
            override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket = error("unused")
            override fun createSocket(host: InetAddress, port: Int): Socket = error("unused")
            override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket = error("unused")
            override fun getDefaultCipherSuites() = arrayOf("TLS_DEFAULT")
            override fun getSupportedCipherSuites() = arrayOf("TLS_SUPPORTED")
        }
        val factory = PinnedSslSocketFactory(
            delegate, "cdn.example", 8443, addresses, 100,
            PinnedSocketConnector { address, _, _ -> connectedAddress = address; rawSocket }
        )

        assertSame(rawSocket, factory.createSocket("ignored.example", 1))
        assertEquals(addresses.first(), connectedAddress)
        assertSame(rawSocket, wrappedSocket)
        assertEquals("cdn.example", wrappedHost)
        assertEquals(8443, wrappedPort)
        assertEquals(true, wrappedAutoClose)
        assertEquals(listOf("TLS_DEFAULT"), factory.defaultCipherSuites.toList())
        assertEquals(listOf("TLS_SUPPORTED"), factory.supportedCipherSuites.toList())
    }
}
