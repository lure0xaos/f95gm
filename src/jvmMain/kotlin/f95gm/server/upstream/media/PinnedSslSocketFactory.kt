package f95gm.server.upstream.media

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

internal fun interface PinnedSocketConnector {
    fun connect(address: InetAddress, port: Int, timeoutMillis: Int): Socket
}

internal class PinnedAddressConnector(
    private val connector: PinnedSocketConnector
) {
    fun connect(addresses: List<InetAddress>, port: Int, timeoutMillis: Int): Socket {
        var lastFailure: IOException? = null
        for (address in addresses) {
            try {
                return connector.connect(address, port, timeoutMillis)
            } catch (error: IOException) {
                lastFailure = error
            }
        }
        throw lastFailure ?: IOException("No validated media address was available")
    }
}

/** TLS socket factory that keeps the validated address while preserving hostname verification. */
internal class PinnedSslSocketFactory(
    private val delegate: SSLSocketFactory,
    private val host: String,
    private val port: Int,
    private val addresses: List<InetAddress>,
    private val connectTimeoutMillis: Int,
    private val socketConnector: PinnedSocketConnector = PinnedSocketConnector { address, socketPort, timeoutMillis ->
        val socket = SocketFactory.getDefault().createSocket()
        try {
            socket.connect(InetSocketAddress(address, socketPort), timeoutMillis)
            socket
        } catch (error: IOException) {
            socket.close()
            throw error
        }
    }
) : SSLSocketFactory() {
    private fun pinnedSocket(): Socket = PinnedAddressConnector(socketConnector)
        .connect(addresses, port, connectTimeoutMillis)

    override fun createSocket(host: String, port: Int): Socket = delegate.createSocket(pinnedSocket(), this.host, this.port, true)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket = delegate.createSocket(pinnedSocket(), this.host, this.port, true)
    override fun createSocket(address: InetAddress, port: Int): Socket = delegate.createSocket(pinnedSocket(), this.host, this.port, true)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket = delegate.createSocket(pinnedSocket(), this.host, this.port, true)
    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket = delegate.createSocket(socket, this.host, this.port, autoClose)
    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
}
